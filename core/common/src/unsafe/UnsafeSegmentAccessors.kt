/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Segment
import kotlinx.io.SegmentSetContext
import kotlinx.io.UnsafeIoApi

@UnsafeIoApi
public object UnsafeSegmentAccessors {
    /**
     * Writes [value] to a [segment] at [index]-position past the last readable byte of the segment.
     *
     * [index]'s value must be in range `[0, Segment.capacity)`
     *
     * Segments store data inside a container (for example, [ByteArray]).
     * Here's how data lays out in such containers:
     * ```
     * Segment.data: [......XXXXXXXXXXX..........]
     *               ^      ^          ^         \_ end of the container
     *               |      |          |
     *         start of the |     end of the readable data
     *           container  |     and start of the writable area
     *                      |          (a.k.a. limit)
     *                  beginning of
     *                the readable data
     *                  (a.k.a. pos)
     * ```
     *
     * [index] value is relative to the beginning of the writable area (i.e.`limit`) and should not exceed
     * [Segment.capacity] (i.e. space between the `limit` and the end of the container).
     */
    @Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
    public inline fun setUnsafe(context: SegmentSetContext, segment: Segment, index: Int, value: Byte) {
        segment.setUnchecked(index, value)
    }

    /**
     * Returns byte located at [index]-position within [segment].
     *
     * [index] value should be in range `[0, Segment.size)`.
     *
     * Refer to [setUnsafe] documentation for details on offsets within [Segment].
     */
    public fun getUnsafe(segment: Segment, index: Int) : Byte {
        return segment.getUnchecked(index)
    }
}
