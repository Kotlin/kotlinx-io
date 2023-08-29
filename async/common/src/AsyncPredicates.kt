/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.indexOf

public interface DataSupplierA {
    public suspend fun fetchAtLeast(minBytes: Long = 1): Boolean
}

public interface DataSupplierB {
    public suspend fun fetch(): Boolean
}

/**
 * An interface representing a condition that should be met to return
 * from [AsyncSource.await] or [AsyncSource.tryAwait].
 *
 * The predicate receives the [AsyncSource.buffer] and a callback to fetch more data from [AsyncSource]'s
 * underlying source.
 *
 * AwaitPredicate implementations should abstain from modifying the buffer as it may not be expected by
 * users supplying a predicate to await data from an [AsyncSource].
 *
 * @sample kotlinx.io.async.samples.KotlinxIOAsyncPredicateSample.predicateSample
 */
public interface AwaitPredicate {
    /**
     * Returns `true` if data within [buffer] met a criterion represented by a particular [AwaitPredicate] instance.
     * If the buffer does not contain enough data to satisfy the criterion, the predicate may call
     * potentially suspending [fetchMore] that will try to fetch more data into the [buffer].
     * A value returned by the [fetchMore] indicate if a source exhausted (in case of `true`)
     * or not (in case if `false`).
     *
     * Implementations performing long-running computations are responsible for ensuring that the coroutine
     * was not canceled.
     *
     * @param buffer the buffer whose data should be checked for meeting some criterion.
     * @param fetchMore the callback to fetch more data into the [buffer].
     *
     * @throws kotlinx.coroutines.CancellationException if a coroutine executing this method was canceled.
     */
    public suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean

    public suspend fun applyA(buffer: Buffer, fetchMore: DataSupplierA): Boolean

    public suspend fun applyB(buffer: Buffer, fetchMore: DataSupplierB): Boolean

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

    override suspend fun applyA(buffer: Buffer, fetchMore: DataSupplierA): Boolean {
        while (fetchMore.fetchAtLeast()) {
            // wait until source exhausted
        }
        return true
    }

    override suspend fun applyB(buffer: Buffer, fetchMore: DataSupplierB): Boolean {
        while (fetchMore.fetch()) {
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

    override suspend fun applyA(buffer: Buffer, fetchMore: DataSupplierA): Boolean {
        if (buffer.size >= bytesCount) return true
        return fetchMore.fetchAtLeast(bytesCount - buffer.size)
    }

    override suspend fun applyB(buffer: Buffer, fetchMore: DataSupplierB): Boolean {
        while (buffer.size < bytesCount && fetchMore.fetch()) {
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

    override suspend fun applyA(buffer: Buffer, fetchMore: DataSupplierA): Boolean {
        do {
            if (buffer.indexOf(byte, startOffset, maxLookAhead) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore.fetchAtLeast())
        return true
    }

    override suspend fun applyB(buffer: Buffer, fetchMore: DataSupplierB): Boolean {
        do {
            if (buffer.indexOf(byte, startOffset, maxLookAhead) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore.fetch())
        return true
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

    override suspend fun applyA(buffer: Buffer, fetchMore: DataSupplierA): Boolean {
        do {
            if (buffer.indexOf(string, startOffset/*, maxLookAhead*/) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore.fetchAtLeast())
        return false
    }

    override suspend fun applyB(buffer: Buffer, fetchMore: DataSupplierB): Boolean {
        do {
            if (buffer.indexOf(string, startOffset/*, maxLookAhead*/) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore.fetch())
        return false
    }
}
