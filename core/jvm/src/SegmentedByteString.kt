/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2015 Square, Inc.
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
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal actual class SegmentedByteString internal actual constructor(
  @Transient internal actual val segments: Array<ByteArray>,
  @Transient internal actual val directory: IntArray
) : ByteString(EMPTY.data) {

  override fun string(charset: Charset) = toByteString().string(charset)

  override fun base64() = toByteString().base64()

  override fun hex() = toByteString().hex()

  override fun toAsciiLowercase() = toByteString().toAsciiLowercase()

  override fun toAsciiUppercase() = toByteString().toAsciiUppercase()

//  override fun digest(algorithm: String): ByteString {
//    val digestBytes = MessageDigest.getInstance(algorithm).run {
//      forEachSegment { data, offset, byteCount ->
//        update(data, offset, byteCount)
//      }
//      digest()
//    }
//    return ByteString(digestBytes)
//  }
//
//  override fun hmac(algorithm: String, key: ByteString): ByteString {
//    try {
//      val mac = Mac.getInstance(algorithm)
//      mac.init(SecretKeySpec(key.toByteArray(), algorithm))
//      forEachSegment { data, offset, byteCount ->
//        mac.update(data, offset, byteCount)
//      }
//      return ByteString(mac.doFinal())
//    } catch (e: InvalidKeyException) {
//      throw IllegalArgumentException(e)
//    }
//  }

  override fun base64Url() = toByteString().base64Url()

  override fun substring(beginIndex: Int, endIndex: Int): ByteString =
    commonSubstring(beginIndex, endIndex)

  override fun internalGet(pos: Int): Byte = commonInternalGet(pos)

  override fun getSize() = commonGetSize()

  override fun toByteArray(): ByteArray = commonToByteArray()

  override fun asByteBuffer(): ByteBuffer = ByteBuffer.wrap(toByteArray()).asReadOnlyBuffer()

  @Throws(IOException::class)
  override fun write(out: OutputStream) {
    forEachSegment { data, offset, byteCount ->
      out.write(data, offset, byteCount)
    }
  }

  override fun write(buffer: Buffer, offset: Int, byteCount: Int): Unit =
    commonWrite(buffer, offset, byteCount)

  override fun rangeEquals(
    offset: Int,
    other: ByteString,
    otherOffset: Int,
    byteCount: Int
  ): Boolean = commonRangeEquals(offset, other, otherOffset, byteCount)

  override fun rangeEquals(
    offset: Int,
    other: ByteArray,
    otherOffset: Int,
    byteCount: Int
  ): Boolean = commonRangeEquals(offset, other, otherOffset, byteCount)

  override fun copyInto(
    offset: Int,
    target: ByteArray,
    targetOffset: Int,
    byteCount: Int
  ) = commonCopyInto(offset, target, targetOffset, byteCount)

  override fun indexOf(other: ByteArray, fromIndex: Int) = toByteString().indexOf(other, fromIndex)

  override fun lastIndexOf(other: ByteArray, fromIndex: Int) = toByteString().lastIndexOf(
    other,
    fromIndex
  )

  /** Returns a copy as a non-segmented byte string.  */
  private fun toByteString() = ByteString(toByteArray())

  override fun internalArray() = toByteArray()

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  override fun toString() = toByteString().toString()

  @Suppress("unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN") // For Java Serialization.
  private fun writeReplace(): Object = toByteString() as Object
}