/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

import kotlinx.io.internal.*
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.sign

internal inline fun OutputStream.write(segment: Segment, fromIndex: Int, bytesCount: Int) {
  when (segment) {
    is ByteArraySegment -> this.write(segment.data, fromIndex, bytesCount)
    is ByteArraySegment2 -> this.write(segment.data, fromIndex, bytesCount)
    is ByteArraySegment3 -> this.write(segment.data, fromIndex, bytesCount)
    is ByteArraySegment4 -> this.write(segment.data, fromIndex, bytesCount)
    else -> segment.forEach(fromIndex, fromIndex + bytesCount) { this.write(it.toInt()) }
  }
}

internal inline fun InputStream.read(segment: Segment, fromIndex: Int, byteCount: Int): Int {
  return when (segment) {
    is ByteArraySegment -> this.read(segment.data, fromIndex, byteCount)
    is ByteArraySegment2 -> this.read(segment.data, fromIndex, byteCount)
    is ByteArraySegment3 -> this.read(segment.data, fromIndex, byteCount)
    is ByteArraySegment4 -> this.read(segment.data, fromIndex, byteCount)
    else -> {
      var bytesRead = 0
      for (idx in fromIndex until fromIndex + byteCount) {
        val b = read()
        if (b == -1) break
        bytesRead++
        segment[idx] = b.toByte()
      }
      bytesRead
    }
  }
}

internal inline fun ByteBuffer.put(segment: Segment, fromIndex: Int, byteCount: Int) {
  when (segment) {
    is ByteArraySegment -> this.put(segment.data, fromIndex, byteCount)
    is ByteArraySegment2 -> this.put(segment.data, fromIndex, byteCount)
    is ByteArraySegment3 -> this.put(segment.data, fromIndex, byteCount)
    is ByteArraySegment4 -> this.put(segment.data, fromIndex, byteCount)
    else -> segment.forEach(fromIndex, fromIndex + byteCount) { this.put(it) }
  }
}

internal inline fun ByteBuffer.get(segment: Segment, fromIndex: Int, byteCount: Int) {
  when (segment) {
    is ByteArraySegment -> this.get(segment.data, fromIndex, byteCount)
    is ByteArraySegment2 -> this.get(segment.data, fromIndex, byteCount)
    is ByteArraySegment3 -> this.get(segment.data, fromIndex, byteCount)
    is ByteArraySegment4 -> this.get(segment.data, fromIndex, byteCount)
    else -> segment.forEachSet(fromIndex, fromIndex + byteCount) {
      return@forEachSet this.get()
    }
  }
}

internal actual fun Segment.copyInto(dst: Segment, dstOffset: Int, startIndex: Int, endIndex: Int) {
  var startIndex = startIndex
  when (this) {
    is ByteArraySegment -> when (dst) {
      is ByteArraySegment -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment2 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment3 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment4 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      else -> dst.forEachSet(dstOffset, dstOffset + endIndex - startIndex) { data[startIndex++] }
    }
    is ByteArraySegment2 -> when (dst) {
      is ByteArraySegment -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment2 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment3 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment4 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      else -> dst.forEachSet(dstOffset, dstOffset + endIndex - startIndex) { data[startIndex++] }
    }
    is ByteArraySegment3 -> when (dst) {
      is ByteArraySegment -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment2 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment3 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment4 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      else -> dst.forEachSet(dstOffset, dstOffset + endIndex - startIndex) { data[startIndex++] }
    }
    is ByteArraySegment4 -> when (dst) {
      is ByteArraySegment -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment2 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment3 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      is ByteArraySegment4 -> data.copyInto(dst.data, dstOffset, startIndex, endIndex)
      else -> dst.forEachSet(dstOffset, dstOffset + endIndex - startIndex) { data[startIndex++] }
    }
    else -> dst.forEachSet(dstOffset, dstOffset + endIndex - startIndex) { this[startIndex++] }
  }
}

internal actual fun Segment.copyInto(dst: Segment, startIndex: Int, endIndex: Int) {
  copyInto(dst, 0, startIndex, endIndex)
}

internal fun Segment.messageDigest(digest: MessageDigest) {
  when (this) {
    is ByteArraySegment -> digest.update(data, pos, limit - pos)
    is ByteArraySegment2 -> digest.update(data, pos, limit - pos)
    is ByteArraySegment3 -> digest.update(data, pos, limit - pos)
    is ByteArraySegment4 -> digest.update(data, pos, limit - pos)
    else -> forEach { digest.update(it) }
  }
}

internal fun Segment.toString(fromIndex: Int, byteCount: Int, charset: Charset): String = when (this) {
  is ByteArraySegment -> String(data, fromIndex, byteCount, charset)
  is ByteArraySegment2 -> String(data, fromIndex, byteCount, charset)
  is ByteArraySegment3 -> String(data, fromIndex, byteCount, charset)
  is ByteArraySegment4 -> String(data, fromIndex, byteCount, charset)
  else -> {
    val byteArray = ByteArray(byteCount)
    var idx = 0
    forEach(fromIndex, fromIndex + byteCount) { byteArray[idx++] = it }
    String(byteArray, charset)
  }
}
actual inline fun Segment.copyInto(dst: ByteArray, dstPos: Int, fromIndex: Int, toIndex: Int) {
  when (this) {
    is ByteArraySegment -> data.copyInto(dst, dstPos, fromIndex, toIndex)
    is ByteArraySegment2 -> data.copyInto(dst, dstPos, fromIndex, toIndex)
    is ByteArraySegment3 -> data.copyInto(dst, dstPos, fromIndex, toIndex)
    is ByteArraySegment4 -> data.copyInto(dst, dstPos, fromIndex, toIndex)
    else -> {
      var idx = dstPos
      forEach(fromIndex, toIndex) { dst[idx++] = it }
    }
  }
}
actual inline fun ByteArray.copyInfo(dst: Segment, offset: Int, toCopy: Int) {
  when (dst) {
    is ByteArraySegment -> this.copyInto(dst.data, dst.limit, offset, offset + toCopy)
    is ByteArraySegment2 -> this.copyInto(dst.data, dst.limit, offset, offset + toCopy)
    is ByteArraySegment3 -> this.copyInto(dst.data, dst.limit, offset, offset + toCopy)
    is ByteArraySegment4 -> this.copyInto(dst.data, dst.limit, offset, offset + toCopy)
    else -> {
      var idx = offset
      dst.forEachSet(dst.limit, dst.limit + toCopy) { this[idx++] }
    }
  }
}


actual class Buffer actual constructor(actual val pool: SegmentPool) : Source, Sink, Cloneable, ByteChannel {
  @JvmField internal actual var head: Segment? = null

  @get:JvmName("size")
  actual var size: Long = 0L
    internal set

  override fun buffer() = this

  actual override val buffer get() = this

  override fun outputStream(): OutputStream {
    return object : OutputStream() {
      override fun write(b: Int) {
        writeByte(b)
      }

      override fun write(data: ByteArray, offset: Int, byteCount: Int) {
        this@Buffer.write(data, offset, byteCount)
      }

      override fun flush() {}

      override fun close() {}

      override fun toString(): String = "${this@Buffer}.outputStream()"
    }
  }

  actual override fun emitCompleteSegments() = this // Nowhere to emit to!

  actual override fun emit() = this // Nowhere to emit to!

  override fun exhausted() = size == 0L

  @Throws(EOFException::class)
  override fun require(byteCount: Long) {
    if (size < byteCount) throw EOFException()
  }

  override fun request(byteCount: Long) = size >= byteCount

  override fun peek(): Source {
    return PeekSource(this).buffer()
  }

  override fun inputStream(): InputStream {
    return object : InputStream() {
      override fun read(): Int {
        return if (size > 0L) {
          readByte() and 0xff
        } else {
          -1
        }
      }

      override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        return this@Buffer.read(sink, offset, byteCount)
      }

      override fun available() = minOf(size, Integer.MAX_VALUE).toInt()

      override fun close() {}

      override fun toString() = "${this@Buffer}.inputStream()"
    }
  }

  /** Copy `byteCount` bytes from this, starting at `offset`, to `out`. */
  @Throws(IOException::class)
  @JvmOverloads
  fun copyTo(
    out: OutputStream,
    offset: Long = 0L,
    byteCount: Long = size - offset
  ): Buffer {
    var offset = offset
    var byteCount = byteCount
    checkOffsetAndCount(size, offset, byteCount)
    if (byteCount == 0L) return this

    // Skip segments that we aren't copying from.
    var s = head
    while (offset >= s!!.limit - s.pos) {
      offset -= (s.limit - s.pos).toLong()
      s = s.next
    }

    // Copy from one segment at a time.
    while (byteCount > 0L) {
      val pos = (s!!.pos + offset).toInt()
      val toCopy = minOf(s.limit - pos, byteCount).toInt()
      out.write(s, pos, toCopy)
      byteCount -= toCopy.toLong()
      offset = 0L
      s = s.next
    }

    return this
  }

  actual fun copyTo(
    out: Buffer,
    offset: Long,
    byteCount: Long
  ): Buffer = commonCopyTo(out, offset, byteCount)

  actual fun copyTo(
    out: Buffer,
    offset: Long
  ): Buffer = copyTo(out, offset, size - offset)

  /** Write `byteCount` bytes from this to `out`. */
  @Throws(IOException::class)
  @JvmOverloads
  fun writeTo(out: OutputStream, byteCount: Long = size): Buffer {
    var byteCount = byteCount
    checkOffsetAndCount(size, 0, byteCount)

    var s = head
    while (byteCount > 0L) {
      val toCopy = minOf(byteCount, s!!.limit - s.pos).toInt()
      out.write(s, s.pos, toCopy)

      s.pos += toCopy
      size -= toCopy.toLong()
      byteCount -= toCopy.toLong()

      if (s.pos == s.limit) {
        val toRecycle = s
        s = toRecycle.pop()
        head = s
        toRecycle.recycle()
      }
    }

    return this
  }

  /** Read and exhaust bytes from `input` into this. */
  @Throws(IOException::class)
  fun readFrom(input: InputStream): Buffer {
    readFrom(input, Long.MAX_VALUE, true)
    return this
  }

  /** Read `byteCount` bytes from `input` into this. */
  @Throws(IOException::class)
  fun readFrom(input: InputStream, byteCount: Long): Buffer {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    readFrom(input, byteCount, false)
    return this
  }

  @Throws(IOException::class)
  private fun readFrom(input: InputStream, byteCount: Long, forever: Boolean) {
    var byteCount = byteCount
    while (byteCount > 0L || forever) {
      val tail = writableSegment(1)
      val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
      val bytesRead = input.read(tail, tail.limit, maxToCopy)
      if (bytesRead == -1) {
        if (tail.pos == tail.limit) {
          // We allocated a tail segment, but didn't end up needing it. Recycle!
          head = tail.pop()
          tail.recycle()
        }
        if (forever) return
        throw EOFException()
      }
      tail.limit += bytesRead
      size += bytesRead.toLong()
      byteCount -= bytesRead.toLong()
    }
  }

  actual fun completeSegmentByteCount(): Long = commonCompleteSegmentByteCount()

  @Throws(EOFException::class)
  override fun readByte(): Byte = commonReadByte()

  @JvmName("getByte")
  actual operator fun get(pos: Long): Byte = commonGet(pos)

  @Throws(EOFException::class)
  override fun readShort(): Short = commonReadShort()

  @Throws(EOFException::class)
  override fun readInt(): Int = commonReadInt()

  @Throws(EOFException::class)
  override fun readLong(): Long = commonReadLong()

  @Throws(EOFException::class)
  override fun readShortLe() = readShort().reverseBytes()

  @Throws(EOFException::class)
  override fun readIntLe() = readInt().reverseBytes()

  @Throws(EOFException::class)
  override fun readLongLe() = readLong().reverseBytes()

  @Throws(EOFException::class)
  override fun readDecimalLong(): Long = commonReadDecimalLong()

  @Throws(EOFException::class)
  override fun readHexadecimalUnsignedLong(): Long = commonReadHexadecimalUnsignedLong()

//  override fun readByteString(): ByteString = commonReadByteString()
//
//  @Throws(EOFException::class)
//  override fun readByteString(byteCount: Long) = commonReadByteString(byteCount)

  override fun select(options: Options): Int = commonSelect(options)

  @Throws(EOFException::class)
  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)

  @Throws(IOException::class)
  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun readUtf8() = readString(size, Charsets.UTF_8)

  @Throws(EOFException::class)
  override fun readUtf8(byteCount: Long) = readString(byteCount, Charsets.UTF_8)

  override fun readString(charset: Charset) = readString(size, charset)

  @Throws(EOFException::class)
  override fun readString(byteCount: Long, charset: Charset): String {
    require(byteCount >= 0 && byteCount <= Integer.MAX_VALUE) { "byteCount: $byteCount" }
    if (size < byteCount) throw EOFException()
    if (byteCount == 0L) return ""

    val s = head!!
    if (s.pos + byteCount > s.limit) {
      // If the string spans multiple segments, delegate to readBytes().
      return String(readByteArray(byteCount), charset)
    }

    val result = s.toString(s.pos, byteCount.toInt(), charset)
    s.pos += byteCount.toInt()
    size -= byteCount

    if (s.pos == s.limit) {
      head = s.pop()
      s.recycle()
    }

    return result
  }

  @Throws(EOFException::class)
  override fun readUtf8Line(): String? = commonReadUtf8Line()

  @Throws(EOFException::class)
  override fun readUtf8LineStrict() = readUtf8LineStrict(Long.MAX_VALUE)

  @Throws(EOFException::class)
  override fun readUtf8LineStrict(limit: Long): String = commonReadUtf8LineStrict(limit)

  @Throws(EOFException::class)
  override fun readUtf8CodePoint(): Int = commonReadUtf8CodePoint()

  override fun readByteArray() = commonReadByteArray()

  @Throws(EOFException::class)
  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)

  override fun read(sink: ByteArray) = commonRead(sink)

  @Throws(EOFException::class)
  override fun readFully(sink: ByteArray) = commonReadFully(sink)

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  @Throws(IOException::class)
  override fun read(sink: ByteBuffer): Int {
    val s = head ?: return -1

    val toCopy = minOf(sink.remaining(), s.limit - s.pos)
    sink.put(s, s.pos, toCopy)

    s.pos += toCopy
    size -= toCopy.toLong()

    if (s.pos == s.limit) {
      head = s.pop()
      s.recycle()
    }

    return toCopy
  }

  actual fun clear() = commonClear()

  @Throws(EOFException::class)
  actual override fun skip(byteCount: Long) = commonSkip(byteCount)

//  actual override fun write(byteString: ByteString): Buffer = commonWrite(byteString)
//
//  actual override fun write(byteString: ByteString, offset: Int, byteCount: Int) =
//    commonWrite(byteString, offset, byteCount)

  actual override fun writeUtf8(string: String): Buffer = writeUtf8(string, 0, string.length)

  actual override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): Buffer =
    commonWriteUtf8(string, beginIndex, endIndex)

  actual override fun writeUtf8CodePoint(codePoint: Int): Buffer =
    commonWriteUtf8CodePoint(codePoint)

  override fun writeString(string: String, charset: Charset) = writeString(
    string, 0, string.length,
    charset
  )

  override fun writeString(
    string: String,
    beginIndex: Int,
    endIndex: Int,
    charset: Charset
  ): Buffer {
    require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
    require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
    require(endIndex <= string.length) { "endIndex > string.length: $endIndex > ${string.length}" }
    if (charset == Charsets.UTF_8) return writeUtf8(string, beginIndex, endIndex)
    val data = string.substring(beginIndex, endIndex).toByteArray(charset)
    return write(data, 0, data.size)
  }

  actual override fun write(source: ByteArray): Buffer = commonWrite(source)

  actual override fun write(
    source: ByteArray,
    offset: Int,
    byteCount: Int
  ): Buffer = commonWrite(source, offset, byteCount)

  @Throws(IOException::class)
  override fun write(source: ByteBuffer): Int {
    val byteCount = source.remaining()
    var remaining = byteCount
    while (remaining > 0) {
      val tail = writableSegment(1)

      val toCopy = minOf(remaining, Segment.SIZE - tail.limit)
      source.get(tail, tail.limit, toCopy)

      remaining -= toCopy
      tail.limit += toCopy
    }

    size += byteCount.toLong()
    return byteCount
  }

  @Throws(IOException::class)
  override fun writeAll(source: RawSource): Long = commonWriteAll(source)

  @Throws(IOException::class)
  actual override fun write(source: RawSource, byteCount: Long): Buffer =
    commonWrite(source, byteCount)

  actual override fun writeByte(b: Int): Buffer = commonWriteByte(b)

  actual override fun writeShort(s: Int): Buffer = commonWriteShort(s)

  actual override fun writeShortLe(s: Int) = writeShort(s.toShort().reverseBytes().toInt())

  actual override fun writeInt(i: Int): Buffer = commonWriteInt(i)

  actual override fun writeIntLe(i: Int) = writeInt(i.reverseBytes())

  actual override fun writeLong(v: Long): Buffer = commonWriteLong(v)

  actual override fun writeLongLe(v: Long) = writeLong(v.reverseBytes())

  actual override fun writeDecimalLong(v: Long): Buffer = commonWriteDecimalLong(v)

  actual override fun writeHexadecimalUnsignedLong(v: Long): Buffer =
    commonWriteHexadecimalUnsignedLong(v)

  internal actual fun writableSegment(minimumCapacity: Int): Segment =
    commonWritableSegment(minimumCapacity)

  override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)

  override fun indexOf(b: Byte) = indexOf(b, 0, Long.MAX_VALUE)

  /**
   * Returns the index of `b` in this at or beyond `fromIndex`, or -1 if this buffer does not
   * contain `b` in that range.
   */
  override fun indexOf(b: Byte, fromIndex: Long) = indexOf(b, fromIndex, Long.MAX_VALUE)

  override fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long = commonIndexOf(b, fromIndex, toIndex)

//  @Throws(IOException::class)
//  override fun indexOf(bytes: ByteString): Long = indexOf(bytes, 0)
//
//  @Throws(IOException::class)
//  override fun indexOf(bytes: ByteString, fromIndex: Long): Long = commonIndexOf(bytes, fromIndex)
//
//  override fun indexOfElement(targetBytes: ByteString) = indexOfElement(targetBytes, 0L)
//
//  override fun indexOfElement(targetBytes: ByteString, fromIndex: Long): Long =
//    commonIndexOfElement(targetBytes, fromIndex)
//
//  override fun rangeEquals(offset: Long, bytes: ByteString) =
//    rangeEquals(offset, bytes, 0, bytes.size)
//
//  override fun rangeEquals(
//    offset: Long,
//    bytes: ByteString,
//    bytesOffset: Int,
//    byteCount: Int
//  ): Boolean = commonRangeEquals(offset, bytes, bytesOffset, byteCount)

  override fun flush() {}

  override fun isOpen() = true

  override fun close() {}

  override fun cancel() {
    // Not cancelable.
  }

  /**
   * Returns the 128-bit MD5 hash of this buffer.
   *
   * MD5 has been vulnerable to collisions since 2004. It should not be used in new code.
   */
  actual fun md5() = digest("MD5")

  /**
   * Returns the 160-bit SHA-1 hash of this buffer.
   *
   * SHA-1 has been vulnerable to collisions since 2017. It should not be used in new code.
   */
  actual fun sha1() = digest("SHA-1")

  /** Returns the 256-bit SHA-256 hash of this buffer. */
  actual fun sha256() = digest("SHA-256")

  /** Returns the 512-bit SHA-512 hash of this buffer. */
  actual fun sha512() = digest("SHA-512")

  private fun digest(algorithm: String): ByteString {
    val messageDigest = MessageDigest.getInstance(algorithm)
    head?.let { head ->
      head.messageDigest(messageDigest)
      var s = head.next!!
      while (s !== head) {
        s.messageDigest(messageDigest)
        s = s.next!!
      }
    }
    return ByteString(messageDigest.digest())
  }

//  /** Returns the 160-bit SHA-1 HMAC of this buffer. */
//  actual fun hmacSha1(key: ByteString) = hmac("HmacSHA1", key)
//
//  /** Returns the 256-bit SHA-256 HMAC of this buffer. */
//  actual fun hmacSha256(key: ByteString) = hmac("HmacSHA256", key)
//
//  /** Returns the 512-bit SHA-512 HMAC of this buffer. */
//  actual fun hmacSha512(key: ByteString) = hmac("HmacSHA512", key)
//
//  private fun hmac(algorithm: String, key: ByteString): ByteString {
//    try {
//      val mac = Mac.getInstance(algorithm)
//      mac.init(SecretKeySpec(key.internalArray(), algorithm))
//      head?.let { head ->
//        mac.update(head.data, head.pos, head.limit - head.pos)
//        var s = head.next!!
//        while (s !== head) {
//          mac.update(s.data, s.pos, s.limit - s.pos)
//          s = s.next!!
//        }
//      }
//      return ByteString(mac.doFinal())
//    } catch (e: InvalidKeyException) {
//      throw IllegalArgumentException(e)
//    }
//  }

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  /**
   * Returns a human-readable string that describes the contents of this buffer. Typically this
   * is a string like `[text=Hello]` or `[hex=0000ffff]`.
   */
  override fun toString() = commonString(size.toInt())

  actual fun copy(): Buffer = commonCopy()

  /** Returns a deep copy of this buffer. */
  public override fun clone(): Buffer = copy()

//  actual fun snapshot(): ByteString = commonSnapshot()
//
//  actual fun snapshot(byteCount: Int): ByteString = commonSnapshot(byteCount)
//
//  @JvmOverloads
//  actual fun readUnsafe(unsafeCursor: UnsafeCursor): UnsafeCursor = commonReadUnsafe(unsafeCursor)
//
//  @JvmOverloads
//  actual fun readAndWriteUnsafe(unsafeCursor: UnsafeCursor): UnsafeCursor =
//    commonReadAndWriteUnsafe(unsafeCursor)

  actual class UnsafeCursor : Closeable {
    @JvmField actual var buffer: Buffer? = null
    @JvmField actual var readWrite: Boolean = false

    internal actual var segment: Segment? = null
    @JvmField actual var offset = -1L
    // @JvmField actual var data: ByteArray? = null
    @JvmField actual var start = -1
    @JvmField actual var end = -1

    actual fun next(): Int = commonNext()

    actual fun seek(offset: Long): Int = commonSeek(offset)

    actual fun resizeBuffer(newSize: Long): Long = commonResizeBuffer(newSize)

    actual fun expandBuffer(minByteCount: Int): Long = commonExpandBuffer(minByteCount)

    actual override fun close() {
      commonClose()
    }
  }
}