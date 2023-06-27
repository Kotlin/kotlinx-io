/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

internal actual object SegmentPool {
    actual val MAX_SIZE: Int = 0

    actual val byteCount: Int = 0

    actual fun take(): Segment = Segment()

    actual fun recycle(segment: Segment) {
    }
}
