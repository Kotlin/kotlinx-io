/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.indexOf


public interface AwaitPredicate {
    public suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean

    public companion object {
        public fun exhausted(): AwaitPredicate = SourceExhausted.instance

        public fun dataAvailable(bytesCount: Long): AwaitPredicate {
            if (bytesCount.countOneBits() == 1) {
                val offset = bytesCount.countTrailingZeroBits()
                if (offset < dataAvailablePredicates.size) {
                    return dataAvailablePredicates[offset].value
                }
            }
            return DataAvailable(bytesCount)
        }

        public fun byteFound(expectedValue: Byte, maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate =
            ByteValuePredicate(expectedValue, maxLookAhead)

        public fun bytesFound(expectedValue: ByteString, maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate =
            SubstringPredicate(expectedValue, maxLookAhead)

        public fun newLineFound(maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate =
            byteFound('\n'.code.toByte(), maxLookAhead)

        private val dataAvailablePredicates = Array<Lazy<AwaitPredicate>>(16) {
            lazy(LazyThreadSafetyMode.NONE) {
                DataAvailable(1L shl it)
            }
        }
    }
}

private class SourceExhausted : AwaitPredicate {
    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        while (fetchMore()) {
            // wait until source exhausted
        }
        return true
    }

    companion object {
        val instance: SourceExhausted by lazy(LazyThreadSafetyMode.NONE) { SourceExhausted() }
    }
}

private class DataAvailable(
    private val bytesCount: Long
) : AwaitPredicate {
    init {
        require(bytesCount > 0) { "The number of bytes should be positive, was: $bytesCount" }
    }

    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        while (buffer.size < bytesCount && fetchMore()) {
            // do nothing
        }
        return buffer.size >= bytesCount
    }

    override fun toString(): String {
        return "DataAvailable(bytesCount=$bytesCount)"
    }
}

private class ByteValuePredicate(
    private val byte: Byte,
    private val maxLookAhead: Long
) : AwaitPredicate {
    private var startOffset = 0L

    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        do {
            if (buffer.indexOf(byte, startOffset, maxLookAhead) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore())
        return false
    }
}

private class SubstringPredicate(
    private val string: ByteString,
    private val maxLookAhead: Long
) : AwaitPredicate {
    private var startOffset = 0L

    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        do {
            if (buffer.indexOf(string, startOffset/*, maxLookAhead*/) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore())
        return false
    }
}
