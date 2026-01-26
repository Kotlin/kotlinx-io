/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io.coroutines

/**
 * A generic interface for decoding a stream of bytes into discrete elements of type [T].
 *
 * Implementations of this interface are responsible for processing input byte arrays, decoding
 * them into meaningful elements, and delivering them to the provided `byteConsumer` function in
 * sequential order. This allows for efficient handling of streaming data and enables
 * processing without requiring the entire stream to be loaded into memory.
 *
 * ## Lifecycle
 *
 * The decoder processes a stream through repeated calls to [decode], followed by a final call
 * to [onClose] when the stream ends. After [onClose] is called, the decoder should not be reused.
 *
 * ## Thread Safety
 *
 * Implementations are not required to be thread-safe. Each decoder instance should be used with
 * a single stream and should not be shared across concurrent coroutines.
 *
 * @param T The type of elements produced by the decoder.
 */
public interface StreamingDecoder<T> {
    /**
     * Decodes a chunk of bytes from the input stream.
     *
     * This method may be called multiple times as data arrives. Implementations should buffer
     * incomplete elements internally and emit complete elements via [byteConsumer].
     *
     * @param bytes The input byte array to decode.
     * @param byteConsumer A suspend function that receives decoded elements.
     */
    public suspend fun decode(bytes: ByteArray, byteConsumer: suspend (T) -> Unit)

    /**
     * Called when the input stream ends, allowing the decoder to emit any remaining buffered data
     * and perform cleanup.
     *
     * After this method is called, the decoder should not be used again.
     *
     * @param byteConsumer A suspend function that receives any final decoded elements.
     */
    public suspend fun onClose(byteConsumer: suspend (T) -> Unit)
}

