/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples


import kotlinx.io.Buffer
import kotlinx.io.async.AsyncSource
import kotlinx.io.async.AwaitPredicate
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.indexOf

class KotlinxIOAsyncPredicateSample {
    suspend fun predicateSample(asyncSource: AsyncSource, parseHeaders: Buffer.() -> Unit) {
        class AwaitHttpBodySeparator : AwaitPredicate {
            private val CRLFSeparator = "\r\n\r\n".encodeToByteString()

            override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
                var offset = 0L
                do {
                    if (buffer.indexOf(CRLFSeparator, startIndex = offset) != -1L) {
                        return true
                    }
                    offset = buffer.size
                } while (fetchMore());
                return false
            }
        }

        if (asyncSource.await(until = AwaitHttpBodySeparator()).getOrThrow()) {
            parseHeaders(asyncSource.buffer)
        } else {
            throw IllegalStateException("The source exhausted before reaching HTTP body separator.")
        }
    }
}
