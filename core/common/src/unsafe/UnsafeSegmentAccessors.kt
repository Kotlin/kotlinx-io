/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Segment
import kotlinx.io.SegmentSetContext

public object UnsafeSegmentAccessors {
    @Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
    public inline fun setUnsafe(context: SegmentSetContext, segment: Segment, index: Int, value: Byte) {
        segment.setUnchecked(index, value)
    }

    public fun getUnsafe(segment: Segment, index: Int) : Byte {
        return segment.getUnchecked(index)
    }
}
