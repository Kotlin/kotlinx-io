/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io.coroutines

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.readByteArray

public const val READ_BUFFER_SIZE: Long = 8196

/**
 * Converts this [RawSource] into a Kotlin [Flow], emitting decoded data using the provided [StreamingDecoder].
 *
 * This function reads data from the source in chunks, decodes it using the provided decoder, and emits
 * the decoded elements downstream. The returned flow is cold and will start reading from the source
 * when collected.
 *
 * ## Lifecycle and Resource Management
 *
 * - The source is automatically closed when the flow completes, fails, or is cancelled
 * - The decoder's [StreamingDecoder.onClose] is always called for cleanup, even on cancellation
 * - On normal completion or [IOException], any remaining buffered data in the decoder is emitted
 * - On cancellation, the decoder is cleaned up but remaining data is discarded
 *
 * ## Backpressure
 *
 * The flow respects structured concurrency and backpressure. Reading from the source is suspended
 * when the downstream collector cannot keep up.
 *
 * @param T The type of elements emitted by the Flow after decoding.
 * @param decoder The [StreamingDecoder] used to decode data read from this source.
 * @param readBufferSize The size of the buffer used for reading from the source. Defaults to [READ_BUFFER_SIZE].
 * @return A cold [Flow] that emits decoded elements of type [T].
 * @throws IOException if an I/O error occurs while reading from the source.
 */
public fun <T> RawSource.asFlow(
    decoder: StreamingDecoder<T>,
    readBufferSize: Long = READ_BUFFER_SIZE
): Flow<T> =
    channelFlow {
        val source = this@asFlow
        val buffer = Buffer()
        var decoderClosed = false
        try {
            source.use { source ->
                while (isActive) {
                    val bytesRead = source.readAtMostTo(buffer, readBufferSize)
                    if (bytesRead == -1L) {
                        break
                    }

                    if (bytesRead > 0L) {
                        val bytes = buffer.readByteArray()
                        buffer.clear()
                        decoder.decode(bytes) {
                            send(it)
                        }
                    }

                    yield() // Giving other coroutines a chance to run
                }
            }
            // Normal completion: emit any remaining buffered data
            decoder.onClose { send(it) }
            decoderClosed = true
        } catch (exception: IOException) {
            // IO error: try to emit remaining data, then close with error
            runCatching { decoder.onClose { send(it) } }.onSuccess { decoderClosed = true }
            throw exception
        } finally {
            // Ensure decoder cleanup even on cancellation or other exceptions
            if (!decoderClosed) {
                withContext(NonCancellable) {
                    runCatching { decoder.onClose { /* discard data, cleanup only */ } }
                }
            }
            buffer.clear()
        }
    }