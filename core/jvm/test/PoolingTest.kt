/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import org.junit.jupiter.api.Assertions.assertFalse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class PoolingTest {
    @Test
    fun segmentSharing() {
        val buffer = Buffer()

        buffer.writeByte(1)
        var poolSize = SegmentPool.byteCount
        buffer.clear()
        // clear should return a segment to a pool, so the pool size should grow
        assertTrue(poolSize < SegmentPool.byteCount)

        buffer.writeByte(1)
        poolSize = SegmentPool.byteCount
        val copy = buffer.copy()
        buffer.clear()
        copy.clear()
        assertTrue(poolSize < SegmentPool.byteCount)

        buffer.writeByte(1)
        poolSize = SegmentPool.byteCount
        val peek = buffer.peek().buffered()
        peek.readByte()
        buffer.clear()
        assertTrue(poolSize < SegmentPool.byteCount)

        buffer.writeByte(1)
        poolSize = SegmentPool.byteCount
        val otherBuffer = Buffer()
        otherBuffer.write(buffer, buffer.size)
        otherBuffer.clear()
        assertTrue(poolSize < SegmentPool.byteCount)
    }

    @Test
    fun secondLevelPoolCapacityTest() {
        val segments = mutableSetOf<Segment>()

        val maxPoolSize = (SegmentPool.MAX_SIZE + SegmentPool.SECOND_LEVEL_POOL_TOTAL_SIZE) / Segment.SIZE
        repeat(maxPoolSize) {
            assertTrue(segments.add(SegmentPool.take()))
        }

        segments.forEach { SegmentPool.recycle(it) }
        repeat(maxPoolSize) {
            assertFalse(segments.add(SegmentPool.take()))
        }
        assertTrue(segments.add(SegmentPool.take()))

        segments.forEach { SegmentPool.recycle(it) }
        segments.clear()
    }

    @Test
    fun contendedUseTest() {
        val threadsCount = Runtime.getRuntime().availableProcessors()
        val segmentsPerThread = SegmentPool.SECOND_LEVEL_POOL_TOTAL_SIZE / Segment.SIZE / threadsCount
        val observedSegments = ConcurrentHashMap<Segment, Any>()

        val pool = Executors.newFixedThreadPool(threadsCount)
        repeat(threadsCount) {
            pool.submit {
                repeat(10000) {
                    val segments = mutableListOf<Segment>()
                    repeat(segmentsPerThread) {
                        val seg = SegmentPool.take()
                        segments.add(seg)
                        observedSegments[seg] = seg
                    }
                    segments.forEach { SegmentPool.recycle(it) }
                }
            }
        }

        val maxPoolSize =
            (SegmentPool.SECOND_LEVEL_POOL_TOTAL_SIZE + SegmentPool.MAX_SIZE * SegmentPool.HASH_BUCKET_COUNT) / Segment.SIZE
        assertTrue(observedSegments.size <= maxPoolSize)
    }

    @Test
    fun contendedUseWithMixedOperationsTest() {
        val threadsCount = Runtime.getRuntime().availableProcessors()
        val segmentsPerThread = SegmentPool.SECOND_LEVEL_POOL_TOTAL_SIZE / Segment.SIZE / threadsCount
        val observedSegments = ConcurrentHashMap<Segment, Any>()

        val pool = Executors.newFixedThreadPool(threadsCount)
        repeat(threadsCount) {
            pool.submit {
                repeat(10000) {
                    val segments = mutableListOf<Segment>()
                    repeat(segmentsPerThread * 2) {
                        when (segments.size) {
                            0 -> {
                                val seg = SegmentPool.take()
                                segments.add(seg)
                                observedSegments[seg] = seg
                            }
                            segmentsPerThread -> SegmentPool.recycle(segments.removeLast())
                            else -> {
                                val rnd = Random.nextDouble()
                                // More segments we have, higher the probability to return one of them back
                                if (rnd > segments.size.toDouble() / segmentsPerThread) {
                                    SegmentPool.recycle(segments.removeLast())
                                } else {
                                    val seg = SegmentPool.take()
                                    segments.add(seg)
                                    observedSegments[seg] = seg
                                }
                            }
                        }

                    }
                    segments.forEach { SegmentPool.recycle(it) }
                }
            }
        }

        val maxPoolSize =
            (SegmentPool.SECOND_LEVEL_POOL_TOTAL_SIZE + SegmentPool.MAX_SIZE * SegmentPool.HASH_BUCKET_COUNT) / Segment.SIZE
        assertTrue(observedSegments.size <= maxPoolSize)
    }
}
