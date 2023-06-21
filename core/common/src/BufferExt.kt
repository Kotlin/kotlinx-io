/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder

/**
 * Creates a byte string containing a copy of all the data from this buffer.
 *
 * This call doesn't consume data from the buffer, but instead copies it.
 */
public fun Buffer.snapshot(): ByteString {
    if (size == 0L) return ByteString.EMPTY

    val bufferSize = this@snapshot.size
    return with (ByteStringBuilder(bufferSize.toInt())) {
        var curr = head
        do {
            check(curr != null) { "Current segment is null" }
            append(curr.data, curr.pos, curr.limit)
            curr = curr.next
        } while (curr !== head)
        toByteString()
    }
}