/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmark

import kotlinx.io.*
import kotlin.random.Random

class RandomSegmentPool : SegmentPool {
    private val random = Random(0)
    override val MAX_SIZE: Int
        get() = 0
    override val byteCount: Int
        get() = 0

    override fun take(): Segment {
        return when (random.nextInt(5)) {
            0 -> DefaultSegmentPool.take()
            // 1 -> ByteArraySegment2(randomSegmentPool)
            // 2 -> ByteArraySegment3(randomSegmentPool)
            // 3 -> ByteArraySegment4(randomSegmentPool)
            else -> DefaultSegmentPool.take()
        }
    }

    override fun recycle(segment: Segment) {
    }
}

val randomSegmentPool = RandomSegmentPool()