/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource

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
