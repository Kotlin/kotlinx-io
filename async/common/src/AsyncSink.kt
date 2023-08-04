/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer

public class AsyncSink(private val sink: AsyncRawSink) : AsyncRawSink {
    public val buffer: Buffer = Buffer()
    override suspend fun write(buffer: Buffer, bytesCount: Long) {
        this.buffer.write(buffer, bytesCount)
    }

    override suspend fun flush() {
        emit()
        sink.flush()
    }

    override suspend fun close() {
        flush()
        sink.close()
    }

    private suspend fun emit() {
        sink.write(buffer, buffer.size)
    }

}
