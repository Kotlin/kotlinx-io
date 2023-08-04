/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples

import kotlinx.io.*
import kotlinx.io.async.AsyncSource
import kotlinx.io.async.AwaitPredicate
import kotlin.test.Test

data class Message(val timestamp: Long, val text: String) {
    companion object
}


suspend fun Message.Companion.fromBson(source: AsyncSource): Message {
    source.await(AwaitPredicate.dataAvailable(4))          // check if the source contains length
    val buffer = source.buffer
    val length = buffer.readIntLe() - 4L
    source.await(AwaitPredicate.dataAvailable(length))     // check if the source contains the whole message

    fun readFieldName(source: Buffer): String {
        val delimiterOffset = source.indexOf(0)            // find offset of the 0-byte terminating the name
        check(delimiterOffset >= 0)                        // indexOf return -1 if value not found
        val fieldName = source.readString(delimiterOffset) // read the string until terminator
        source.skip(1)                                     // skip the terminator
        return fieldName
    }

    // for simplicity, let's assume that the order of fields matches serialization order
    var tag = buffer.readByte().toInt()                     // read the field type
    check(tag == 0x9 && readFieldName(buffer) == "timestamp")
    val timestamp = buffer.readLongLe()                     // read long value
    tag = buffer.readByte().toInt()
    check(tag == 0x2 && readFieldName(buffer) == "text")
    val textLen = buffer.readIntLe() - 1L                   // read string length (it includes the terminator)
    val text = buffer.readString(textLen)                   // read value
    buffer.skip(1)                                          // skip terminator
    buffer.skip(1)                                          // skip end of the document
    return Message(timestamp, text)
}

class ModuleDescriptionSampleTest {
    @Test
    fun sample() {
    }
}
