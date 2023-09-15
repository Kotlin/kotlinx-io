/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty
import kotlinx.io.indexOf

/**
 * An interface representing a condition that should be met to return
 * from [AsyncSource.awaitOrThrow] or [AsyncSource.await].
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

    /**
     * Provides factory methods returning predefined predicates.
     */
    public companion object {
        /**
         * Returns predicate that could be fulfilled only after the source is exhausted.
         */
        public fun exhausted(): AwaitPredicate = SourceExhausted.instance

        /**
         * Returns predicate checking that the number of bytes available
         * for reading from [AsyncSource.buffer] is at least [bytes].
         *
         * If there is not enough data, an attempt to fetch more data will be made.
         *
         * @param bytes the minimum number of bytes that [AsyncSource.buffer] should contain to fulfill the predicate.
         *
         * @throws IllegalArgumentException if [bytes] is negative.
         */
        public fun available(bytes: Long): AwaitPredicate {
            require(bytes >= 0) { "bytes should not be negative: $bytes" }
            if (bytes.countOneBits() == 1) {
                val offset = bytes.countTrailingZeroBits()
                if (offset < dataAvailablePredicates.size) {
                    return dataAvailablePredicates[offset].value
                }
            }
            return DataAvailable(bytes)
        }

        /**
         * Returns predicate that could be fulfilled only if [AsyncSource.buffer] contains [expectedValue]
         * withing its first [maxLookAhead] bytes.
         *
         * If [AsyncSource.buffer] does not contain [expectedValue], but its size is less then [maxLookAhead] bytes
         * then the attempt to fetch more data from the source will be made.
         *
         * @param expectedValue the value to search for.
         * @param maxLookAhead the max length of the buffer's prefix where [expectedValue] will be searched,
         * [Long.MAX_VALUE] by default.
         *
         * @throws IllegalArgumentException if [maxLookAhead] is less or equal to `0`.
         */
        public fun contains(expectedValue: Byte, maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate {
            require(maxLookAhead > 0) { "maxLookAhead should be positive: $maxLookAhead" }
            return ByteValuePredicate(expectedValue, maxLookAhead)
        }

        /**
         * Returns predicate that could be fulfilled only if [AsyncSource.buffer] contains [expectedValue]
         * withing its first [maxLookAhead] bytes.
         *
         * If [AsyncSource.buffer] does not contain [expectedValue], but its size is less then [maxLookAhead] bytes
         * then the attempt to fetch more data from the source will be made.
         *
         * @param expectedValue the value to search for.
         * @param maxLookAhead the max length of the buffer's prefix where [expectedValue] will be searched,
         * [Long.MAX_VALUE] by default.
         *
         * @throws IllegalArgumentException if [maxLookAhead] is less then the length of the [expectedValue].
         * @throws IllegalArgumentException if [expectedValue] is empty.
         */
        public fun contains(expectedValue: ByteString, maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate {
            require(!expectedValue.isEmpty()) { "expected value is empty" }
            require(maxLookAhead >= expectedValue.size) {
                "maxLookAhead should not be smaller than expectedValue size (${expectedValue.size}: $maxLookAhead"
            }
            return SubstringPredicate(expectedValue, maxLookAhead)
        }

        /**
         * Returns predicate that could be fulfilled only if [AsyncSource.buffer] contains newline character (`\n`)
         * within its first [maxLookAhead] bytes.
         *
         * If [AsyncSource.buffer] does not contain newline character, but its size is less then [maxLookAhead] bytes
         * then the attempt to fetch more data from the source will be made.
         *
         * @param maxLookAhead the max length of the buffer's prefix where newline character will be searched,
         * [Long.MAX_VALUE] by default.
         *
         * @throws IllegalArgumentException if [maxLookAhead] is less or equal to `0`.
         */
        public fun newLine(maxLookAhead: Long = Long.MAX_VALUE): AwaitPredicate =
            contains('\n'.code.toByte(), maxLookAhead)

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
            if (buffer.indexOf(string, startOffset, maxLookAhead) != -1L) {
                return true
            }
            startOffset = buffer.size
        } while (maxLookAhead > startOffset && fetchMore())
        return false
    }
}
