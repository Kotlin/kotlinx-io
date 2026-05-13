/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io.coroutines

import kotlinx.io.Buffer
import kotlinx.io.readByteArray

/**
 * A streaming decoder that reads a continuous stream of bytes and separates it into discrete
 * chunks based on a specified delimiter. The default delimiter is the newline character (`'\n'`).
 *
 * This class buffers incoming byte arrays and emits individual byte arrays once a delimiter is
 * encountered. Any remaining bytes in the buffer are emitted when [onClose] is called.
 *
 * ## Example
 *
 * ```kotlin
 * val decoder = DelimitingByteStreamDecoder()
 * val source: RawSource = // ...
 * source.asFlow(decoder).collect { line ->
 *     println("Received: ${line.decodeToString()}")
 * }
 * ```
 *
 * ## Thread Safety
 *
 * This class is **not thread-safe**. Each instance maintains internal mutable state and must
 * not be shared across multiple flows or concurrent coroutines.
 *
 * ## Lifecycle
 *
 * After [onClose] is called, this decoder **cannot be reused**. The internal buffer is closed
 * and the decoder should be discarded.
 *
 * @property delimiter The byte value used as a delimiter to separate the stream into chunks.
 *                      Defaults to the newline character (`'\n'`).
 */
public class DelimitingByteStreamDecoder(
    public val delimiter: Byte = '\n'.code.toByte(),
) : StreamingDecoder<ByteArray> {

    private val buffer = Buffer()

    override suspend fun decode(bytes: ByteArray, byteConsumer: suspend (ByteArray) -> Unit) {
        var startIndex = 0
        for (i in bytes.indices) {
            if (bytes[i] == delimiter) {
                buffer.write(bytes, startIndex, i)
                // flush and clear buffer
                byteConsumer.invoke(buffer.readByteArray())
                startIndex = i + 1
            }
        }
        // Buffer any remaining bytes after the last delimiter
        if (startIndex < bytes.size) {
            buffer.write(bytes, startIndex, bytes.size)
        }
    }

    override suspend fun onClose(byteConsumer: suspend (ByteArray) -> Unit) {
        if (buffer.size > 0) {
            byteConsumer.invoke(buffer.readByteArray())
        }
        buffer.close()
    }
}