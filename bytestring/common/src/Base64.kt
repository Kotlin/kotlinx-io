/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.Default.encode
import kotlin.io.encoding.Base64.Default.encodeToByteArray
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Encodes bytes from the specified [source] byte string or its subrange.
 * Returns a [ByteArray] containing the resulting symbols.
 *
 * If the size of the [source] byte string or its subrange is not an integral multiple of 3,
 * the result is padded with `'='` to an integral multiple of 4 symbols.
 *
 * Each resulting symbol occupies one byte in the returned byte array.
 *
 * Use [encode] to get the output in string form.
 *
 * @param source the byte string to encode bytes from.
 * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @return a [ByteArray] with the resulting symbols.
 */
@ExperimentalEncodingApi
public fun Base64.encodeToByteArray(source: ByteString, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
    return encodeToByteArray(source.getBackingArrayReference(), startIndex, endIndex)
}

/**
 * Encodes bytes from the specified [source] byte string or its subrange and writes resulting symbols into the [destination] array.
 * Returns the number of symbols written.
 *
 * If the size of the [source] byte string or its subrange is not an integral multiple of 3,
 * the result is padded with `'='` to an integral multiple of 4 symbols.
 *
 * @param source the byte string to encode bytes from.
 * @param destination the array to write symbols into.
 * @param destinationOffset the starting index in the [destination] array to write symbols to, 0 by default.
 * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the resulting symbols don't fit into the [destination] array starting at the specified [destinationOffset],
 * or when that index is out of the [destination] array indices range.
 *
 * @return the number of symbols written into [destination] array.
 */
@ExperimentalEncodingApi
public fun Base64.encodeIntoByteArray(
    source: ByteString,
    destination: ByteArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = source.size
): Int {
    return encodeIntoByteArray(source.getBackingArrayReference(), destination, destinationOffset, startIndex, endIndex)
}

/**
 * Encodes bytes from the specified [source] byte string or its subrange.
 * Returns a string with the resulting symbols.
 *
 * If the size of the [source] byte string or its subrange is not an integral multiple of 3,
 * the result is padded with `'='` to an integral multiple of 4 symbols.
 *
 * Use [encodeToByteArray] to get the output in [ByteArray] form.
 *
 * @param source the byte string to encode bytes from.
 * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @return a string with the resulting symbols.
 */
@ExperimentalEncodingApi
public fun Base64.encode(
    source: ByteString,
    startIndex: Int = 0,
    endIndex: Int = source.size
): String {
    return encode(source.getBackingArrayReference(), startIndex, endIndex)
}

/**
 * Encodes bytes from the specified [source] byte string or its subrange and appends resulting symbols to the [destination] appendable.
 * Returns the destination appendable.
 *
 * If the size of the [source] byte string or its subrange is not an integral multiple of 3,
 * the result is padded with `'='` to an integral multiple of 4 symbols.
 *
 * @param source the byte string to encode bytes from.
 * @param destination the appendable to append symbols to.
 * @param startIndex the beginning (inclusive) of the subrange to encode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to encode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @return the destination appendable.
 */
@ExperimentalEncodingApi
public fun <A : Appendable> Base64.encodeToAppendable(
    source: ByteString,
    destination: A,
    startIndex: Int = 0,
    endIndex: Int = source.size
): A {
    return encodeToAppendable(source.getBackingArrayReference(), destination, startIndex, endIndex)
}


/**
 * Decodes symbols from the specified [source] byte string or its subrange.
 * Returns a [ByteArray] containing the resulting bytes.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
 *
 * @param source the byte string to decode symbols from.
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
 *
 * @return a [ByteArray] with the resulting bytes.
 */
@ExperimentalEncodingApi
public fun Base64.decode(source: ByteString, startIndex: Int = 0, endIndex: Int = source.size): ByteArray {
    return decode(source.getBackingArrayReference(), startIndex, endIndex)
}

/**
 * Decodes symbols from the specified [source] char sequence or its substring.
 * Returns a [ByteString] containing the resulting bytes.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
 *
 * @param source the char sequence to decode symbols from.
 * @param startIndex the beginning (inclusive) of the substring to decode, 0 by default.
 * @param endIndex the end (exclusive) of the substring to decode, length of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
 *
 * @return a [ByteArray] with the resulting bytes.
 */
@ExperimentalEncodingApi
public fun Base64.decodeToByteString(source: CharSequence, startIndex: Int = 0, endIndex: Int = source.length): ByteString {
    return ByteString.wrap(decode(source, startIndex, endIndex))
}

/**
 * Decodes symbols from the specified [source] byte string or its subrange and writes resulting bytes into the [destination] array.
 * Returns the number of bytes written.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
 *
 * @param source the byte string to decode symbols from.
 * @param destination the array to write bytes into.
 * @param destinationOffset the starting index in the [destination] array to write bytes to, 0 by default.
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the resulting bytes don't fit into the [destination] array starting at the specified [destinationOffset],
 * or when that index is out of the [destination] array indices range.
 * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
 *
 * @return the number of bytes written into [destination] array.
 */
@ExperimentalEncodingApi
public fun Base64.decodeIntoByteArray(
    source: ByteString,
    destination: ByteArray,
    destinationOffset: Int = 0,
    startIndex: Int = 0,
    endIndex: Int = source.size
): Int {
    return decodeIntoByteArray(source.getBackingArrayReference(), destination, destinationOffset, startIndex, endIndex)
}

/**
 * Decodes symbols from the specified [source] byte string or its subrange.
 * Returns a [ByteString] containing the resulting bytes.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
 *
 * @param source the byte string to decode symbols from.
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
 *
 * @return a [ByteString] with the resulting bytes.
 */
@ExperimentalEncodingApi
public fun Base64.decodeToByteString(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteString {
    return ByteString.wrap(decode(source, startIndex, endIndex))
}

/**
 * Decodes symbols from the specified [source] byte string or its subrange.
 * Returns a [ByteString] containing the resulting bytes.
 *
 * The symbols for decoding are not required to be padded.
 * However, if there is a padding character present, the correct amount of padding character(s) must be present.
 * The padding character `'='` is interpreted as the end of the encoded byte data. Subsequent symbols are prohibited.
 *
 * @param source the byte string to decode symbols from.
 * @param startIndex the beginning (inclusive) of the subrange to decode, 0 by default.
 * @param endIndex the end (exclusive) of the subrange to decode, size of the [source] byte string by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] byte string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalArgumentException when the symbols for decoding are padded incorrectly or there are extra symbols after the padding.
 *
 * @return a [ByteString] with the resulting bytes.
 */
@ExperimentalEncodingApi
public fun Base64.decodeToByteString(source: ByteString, startIndex: Int = 0, endIndex: Int = source.size): ByteString {
    return ByteString.wrap(decode(source.getBackingArrayReference(), startIndex, endIndex))
}
