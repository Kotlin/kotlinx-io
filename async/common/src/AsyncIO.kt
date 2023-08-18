/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlin.coroutines.CoroutineContext

public fun AsyncRawSource.buffered(): AsyncSource = AsyncSource(this)

public suspend fun <T: AsyncRawSink> T.use(block: suspend (T) -> Unit) {
    block(this)
    close()
}

public suspend fun <T: AsyncRawSource> T.use(block: suspend (T) -> Unit) {
    block(this)
    close()
}

public suspend fun AsyncRawSink.writeWithBuffer(block: Buffer.() -> Unit) {
    val buffer = Buffer()
    block(buffer)
    write(buffer, buffer.size)
    flush()
}

/*
public fun AsyncRawSink.asBlocking(): RawSink {
    return object : RawSink {
        override fun write(source: Buffer, byteCount: Long) {
            runBlocking {
                this@asBlocking.write(source, byteCount)
            }
        }

        override fun flush() {
            runBlocking {
                this@asBlocking.flush()
            }
        }

        override fun close() {
            runBlocking {
                this@asBlocking.close()
            }
        }
    }
}

 */

public fun RawSink.asAsync(ctx: CoroutineContext = Dispatchers.Default): AsyncRawSink {
    return object : AsyncRawSink {
        override suspend fun write(source: Buffer, byteCount: Long) {
            withContext(ctx) {
                this@asAsync.write(source, byteCount)
            }
        }

        override suspend fun flush() {
            withContext(ctx) {
                this@asAsync.flush()
            }
        }

        override suspend fun close() {
            withContext(ctx) {
                this@asAsync.close()
            }
        }

        override fun closeAbruptly() {
            this@asAsync.close()
        }
    }
}

/*
public fun AsyncRawSource.asBlocking(): RawSource {
    return object : RawSource {
        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            return runBlocking {
                this@asBlocking.readAtMostTo(sink, byteCount)
            }
        }

        override fun close() {
            runBlocking {
                this@asBlocking.close()
            }
        }
    }
}
 */

public fun RawSource.asAsync(ctx: CoroutineContext = Dispatchers.Default): AsyncRawSource {
    return object : AsyncRawSource {
        override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
            return withContext(ctx) {
                readAtMostTo(sink, byteCount)
            }
        }

        override suspend fun close() {
            withContext(ctx) {
                this@asAsync.close()
            }
        }

        override fun closeAbruptly() {
            this@asAsync.close()
        }
    }
}
