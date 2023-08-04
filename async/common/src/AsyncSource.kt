/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.io.Buffer
import kotlinx.io.IOException

private const val SEGMENT_SIZE: Long = 8192L

public class AsyncSource(private val source: AsyncRawSource) : AsyncRawSource {
    public val buffer: Buffer = Buffer()

    public suspend fun await(until: AwaitPredicate) {
        if (!tryAwait(until)) {
            throw IOException("Predicate could not be fulfilled.")
        }
    }

    public suspend fun tryAwait(until: AwaitPredicate): Boolean {
        if (!currentCoroutineContext().isActive) {
            return false
        }
        return until.apply(buffer) {
            currentCoroutineContext().isActive && source.readAtMostTo(buffer, SEGMENT_SIZE) >= 0
        }
    }

    override suspend fun readAtMostTo(buffer: Buffer, bytesCount: Long): Long {
        return if (this.buffer.exhausted()) {
            source.readAtMostTo(buffer, bytesCount)
        } else {
            this.buffer.readAtMostTo(buffer, bytesCount)
        }
    }

    override fun close() {
        source.close()
    }
}
