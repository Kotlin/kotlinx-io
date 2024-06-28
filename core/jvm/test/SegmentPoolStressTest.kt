/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertTrue

class SegmentPoolStressTest {
    @Test
    fun segmentPoolLeakage() {
        val observedSegments: MutableMap<Segment, Any?> = ConcurrentHashMap<Segment, Any?>()

        val threadsNumber = Runtime.getRuntime().availableProcessors() * 3
        val pool = Executors.newFixedThreadPool(threadsNumber)
        val totalOps = 1000000
        val acc = AtomicLong(0)

        repeat(threadsNumber) {
            pool.submit {
                val a = Any()
                while (acc.getAndIncrement() < totalOps) {
                    val seg = SegmentPool.take()
                    SegmentPool.recycle(seg)
                    observedSegments[seg] = a
                    if (observedSegments.size > threadsNumber * 2) {
                        throw IllegalStateException("Too many segments, may end up with OOME")
                    }
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(30, TimeUnit.SECONDS)

        assertTrue(
            observedSegments.size <= threadsNumber,
            "Expected number of unique segments taken from a pool (${observedSegments.size}) to be below $threadsNumber"
        )
    }
}
