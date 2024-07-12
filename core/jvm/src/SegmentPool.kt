/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

import kotlinx.io.SegmentPool.HASH_BUCKET_COUNT
import kotlinx.io.SegmentPool.LOCK
import kotlinx.io.SegmentPool.MAX_SIZE
import kotlinx.io.SegmentPool.recycle
import kotlinx.io.SegmentPool.take
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Precise [SegmentCopyTracker] implementation tracking a number of shared segment copies.
 * Every [addCopy] call increments the counter, every [removeCopy] decrements it.
 *
 * After calling [removeCopy] the same number of time [addCopy] was called, tracker returns to the unshared state.
 *
 * The class is internal for testing only.
 */
internal class RefCountingCopyTracker : SegmentCopyTracker() {
    companion object {
        @JvmStatic
        private val fieldUpdater = AtomicIntegerFieldUpdater.newUpdater(RefCountingCopyTracker::class.java, "copyCount")
    }

    @Volatile
    private var copyCount: Int = 0

    override val shared: Boolean
        get() {
            return copyCount > 0
        }

    override fun addCopy() {
        fieldUpdater.incrementAndGet(this)
    }

    override fun removeCopy(): Boolean {
        // The value could not be incremented from `0` under the race,
        // so once it zero, it remains zero in the scope of this call.
        if (copyCount == 0) return false

        val updatedValue = fieldUpdater.decrementAndGet(this)
        // If there are several copies, the last decrement will update copyCount from 0 to -1.
        // That would be the last standing copy, and we can recycle it.
        // If, however, the decremented value falls below -1, it's an error as there were more
        // `removeCopy` than `addCopy` calls.
        if (updatedValue >= 0) return true
        check(updatedValue == -1) { "Shared copies count is negative: ${updatedValue + 1}" }
        copyCount = 0
        return false
    }
}

/**
 * This class pools segments in a lock-free singly-linked stack. Though this code is lock-free it
 * does use a sentinel [LOCK] value to defend against races. To reduce the contention, the pool consists
 * of several buckets (see [HASH_BUCKET_COUNT]), each holding a reference to its own segments stack.
 * Every [take] or [recycle] choose one of the buckets depending on a [Thread.currentThread]'s [Thread.getId].
 *
 * On [take], a caller swaps the stack's next pointer with the [LOCK] sentinel. If the stack was
 * not already locked, the caller replaces the head node with its successor.
 *
 * On [recycle], a caller swaps the head with a new node whose successor is the replaced head.
 *
 * On conflict, operations are retried until they succeed.
 *
 * This tracks the number of bytes in each linked list in its [Segment.limit] property. Each element
 * has a limit that's one segment size greater than its successor element. The maximum size of the
 * pool is a product of [MAX_SIZE] and [HASH_BUCKET_COUNT].
 *
 * [MAX_SIZE] is kept relatively small to avoid excessive memory consumption in case of a large [HASH_BUCKET_COUNT].
 * For better handling of scenarios with high segments demand, an optional second-level pool could be enabled
 * by setting up a value of `kotlinx.io.pool.size.bytes` system property.
 *
 * The second-level pool use half of the [HASH_BUCKET_COUNT] and if an initially selected bucket if empty on [take] or
 * full or [recycle], all other buckets will be inspected before finally giving up (which means allocating a new segment
 * on [take], or loosing a reference to a segment on [recycle]).
 * That pool is used as a backup in case when [take] or [recycle] failed due to
 * an empty or exhausted segments chain in a corresponding bucket (one of [HASH_BUCKET_COUNT] buckets).
 */
internal actual object SegmentPool {
    /** The maximum number of bytes to pool per hash bucket. */
    // TODO: Is 64 KiB a good maximum size? Do we ever have that many idle segments?
    actual val MAX_SIZE = 64 * 1024 // 64 KiB.

    /** A sentinel segment to indicate that the linked list is currently being modified. */
    private val LOCK =
        Segment.new(ByteArray(0), pos = 0, limit = 0, copyTracker = null, owner = false)

    /**
     * The number of hash buckets. This number needs to balance keeping the pool small and contention
     * low. We use the number of processors rounded up to the nearest power of two. For example a
     * machine with 6 cores will have 8 hash buckets.
     */
    private val HASH_BUCKET_COUNT =
        Integer.highestOneBit(Runtime.getRuntime().availableProcessors() * 2 - 1)

    private val HASH_BUCKET_COUNT_L2 = (HASH_BUCKET_COUNT / 2).coerceAtLeast(1)

    // For now, keep things on Android as they were before, but on JVM - use second level cache.
    // See https://developer.android.com/reference/java/lang/System#getProperties() for property name.
    private val DEFAULT_SECOND_LEVEL_POOL_TOTAL_SIZE = when (System.getProperty("java.vm.name")) {
        "Dalvik" -> "0"
        else -> "4194304" // 4MB
    }

    private val SECOND_LEVEL_POOL_TOTAL_SIZE =
        System.getProperty("kotlinx.io.pool.size.bytes", DEFAULT_SECOND_LEVEL_POOL_TOTAL_SIZE)
            .toIntOrNull()?.coerceAtLeast(0) ?: 0

    private val SECOND_LEVEL_POOL_BUCKET_SIZE =
        (SECOND_LEVEL_POOL_TOTAL_SIZE / HASH_BUCKET_COUNT).coerceAtLeast(Segment.SIZE)

    /**
     * Hash buckets each contain a singly-linked list of segments. The index/key is a hash function of
     * thread ID because it may reduce contention or increase locality.
     *
     * We don't use [ThreadLocal] because we don't know how many threads the host process has and we
     * don't want to leak memory for the duration of a thread's life.
     */
    private val hashBuckets: AtomicReferenceArray<Segment?> = AtomicReferenceArray(HASH_BUCKET_COUNT)
    private val hashBucketsL2: AtomicReferenceArray<Segment?> = AtomicReferenceArray(HASH_BUCKET_COUNT_L2)

    actual val byteCount: Int
        get() {
            val first = hashBuckets[l1BucketId()] ?: return 0
            return first.limit
        }

    @JvmStatic
    actual fun take(): Segment {
        val buckets = hashBuckets
        val bucketId = l1BucketId()

        while (true) {
            when (val first = buckets.getAndSet(bucketId, LOCK)) {
                LOCK -> {
                    // We didn't acquire the lock. Let's try again
                    continue
                }

                null -> {
                    // We acquired the lock but the pool was empty.
                    // Unlock the bucket and either try to acquire a segment from the second level cache,
                    // or, if the second level cache is disabled, allocate a brand-new segment.
                    buckets.set(bucketId, null)

                    if (SECOND_LEVEL_POOL_TOTAL_SIZE > 0) {
                        return takeL2()
                    }

                    return Segment.new()
                }

                else -> {
                    // We acquired the lock and the pool was not empty. Pop the first element and return it.
                    buckets.set(bucketId, first.next)
                    first.next = null
                    first.limit = 0
                    return first
                }
            }
        }
    }

    @JvmStatic
    private fun takeL2(): Segment {
        val buckets = hashBuckets
        var bucket = l2BucketId()
        var attempts = 0
        while (true) {
            when (val first = buckets.getAndSet(bucket, LOCK)) {
                LOCK -> {
                    // We didn't acquire the lock, retry
                    continue
                }

                null -> {
                    // We acquired the lock but the pool was empty.
                    // Unlock the current bucket and select a new one.
                    // If all buckets were already scanned, allocate a new segment.
                    buckets.set(bucket, null)

                    if (attempts < HASH_BUCKET_COUNT_L2) {
                        bucket = (bucket + 1) and (HASH_BUCKET_COUNT_L2 - 1)
                        attempts++
                        continue
                    }

                    return Segment.new()
                }

                else -> {
                    // We acquired the lock and the pool was not empty. Pop the first element and return it.
                    buckets.set(bucket, first.next)
                    first.next = null
                    first.limit = 0
                    return first
                }
            }
        }
    }

    @JvmStatic
    actual fun recycle(segment: Segment) {
        require(segment.next == null && segment.prev == null)
        if (segment.copyTracker?.removeCopy() == true) return // This segment cannot be recycled.

        val buckets = hashBuckets
        val bucketId = l1BucketId()

        segment.pos = 0
        segment.owner = true

        while (true) {
            val first = buckets[bucketId]
            if (first === LOCK) continue // A take() is currently in progress.
            val firstLimit = first?.limit ?: 0
            if (firstLimit >= MAX_SIZE) {
                // L1 pool is full.
                if (SECOND_LEVEL_POOL_TOTAL_SIZE > 0) {
                    recycleL2(segment)
                }
                return
            }

            segment.next = first
            segment.limit = firstLimit + Segment.SIZE

            if (buckets.compareAndSet(bucketId, first, segment)) {
                return
            }
        }
    }

    @JvmStatic
    private fun recycleL2(segment: Segment) {
        segment.pos = 0
        segment.owner = true

        var bucket = l2BucketId()
        val buckets = hashBucketsL2
        var attempts = 0

        while (true) {
            val first = buckets[bucket]
            if (first === LOCK) continue // A take() is currently in progress.
            val firstLimit = first?.limit ?: 0
            if (firstLimit + Segment.SIZE > SECOND_LEVEL_POOL_BUCKET_SIZE) {
                // The current bucket is full, try to find another one and return the segment there.
                if (attempts < HASH_BUCKET_COUNT_L2) {
                    attempts++
                    bucket = (bucket + 1) and (HASH_BUCKET_COUNT_L2 - 1)
                    continue
                }
                // L2 pool is full.
                return
            }

            segment.next = first
            segment.limit = firstLimit + Segment.SIZE

            if (buckets.compareAndSet(bucket, first, segment)) {
                return
            }
        }
    }

    @JvmStatic
    actual fun tracker(): SegmentCopyTracker = RefCountingCopyTracker()

    private fun l1BucketId() = bucketId (HASH_BUCKET_COUNT - 1L)

    private fun l2BucketId() = bucketId (HASH_BUCKET_COUNT_L2 - 1L)

    private fun bucketId(mask: Long): Int {
        // Get a value in [0..HASH_BUCKET_COUNT_L2) based on the current thread.
        @Suppress("DEPRECATION") // TODO: switch to threadId after JDK19
        return (Thread.currentThread().id and mask).toInt()
    }
}
