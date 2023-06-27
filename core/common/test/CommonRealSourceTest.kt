/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests solely for the behavior of RealBufferedSource's implementation. For generic
 * BufferedSource behavior, use BufferedSourceTest.
 */
@OptIn(InternalIoApi::class)
class CommonRealSourceTest {
  @Test fun indexOfStopsReadingAtLimit() {
    val buffer = Buffer().also { it.writeUtf8("abcdef") }
    val bufferedSource = (
      object : RawSource by buffer {
        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
          return buffer.readAtMostTo(sink, minOf(1, byteCount))
        }
      }
      ).buffered()

    assertEquals(6, buffer.size)
    assertEquals(-1, bufferedSource.indexOf('e'.code.toByte(), 0, 4))
    assertEquals(2, buffer.size)
  }

  @Test fun requireTracksBufferFirst() {
    val source = Buffer()
    source.writeUtf8("bb")

    val bufferedSource = (source as RawSource).buffered()
    bufferedSource.buffer.writeUtf8("aa")

    bufferedSource.require(2)
    assertEquals(2, bufferedSource.buffer.size)
    assertEquals(2, source.size)
  }

  @Test fun requireIncludesBufferBytes() {
    val source = Buffer()
    source.writeUtf8("b")

    val bufferedSource = (source as RawSource).buffered()
    bufferedSource.buffer.writeUtf8("a")

    bufferedSource.require(2)
    assertEquals("ab", bufferedSource.buffer.readUtf8(2))
  }

  @Test fun requireInsufficientData() {
    val source = Buffer()
    source.writeUtf8("a")

    val bufferedSource = (source as RawSource).buffered()

    assertFailsWith<EOFException> {
      bufferedSource.require(2)
    }
  }

  @Test fun requireReadsOneSegmentAtATime() {
    val source = Buffer()
    source.writeUtf8("a".repeat(Segment.SIZE))
    source.writeUtf8("b".repeat(Segment.SIZE))

    val bufferedSource = (source as RawSource).buffered()

    bufferedSource.require(2)
    assertEquals(Segment.SIZE.toLong(), source.size)
    assertEquals(Segment.SIZE.toLong(), bufferedSource.buffer.size)
  }

  @Test fun skipReadsOneSegmentAtATime() {
    val source = Buffer()
    source.writeUtf8("a".repeat(Segment.SIZE))
    source.writeUtf8("b".repeat(Segment.SIZE))
    val bufferedSource = (source as RawSource).buffered()
    bufferedSource.skip(2)
    assertEquals(Segment.SIZE.toLong(), source.size)
    assertEquals(Segment.SIZE.toLong() - 2L, bufferedSource.buffer.size)
  }

  @Test fun skipTracksBufferFirst() {
    val source = Buffer()
    source.writeUtf8("bb")

    val bufferedSource = (source as RawSource).buffered()
    bufferedSource.buffer.writeUtf8("aa")

    bufferedSource.skip(2)
    assertEquals(0, bufferedSource.buffer.size)
    assertEquals(2, source.size)
  }

  @Test fun operationsAfterClose() {
    val source = Buffer()
    val bufferedSource = (source as RawSource).buffered()
    bufferedSource.close()

    // Test a sample set of methods.
    assertFailsWith<IllegalStateException> { bufferedSource.indexOf(1.toByte()) }
    assertFailsWith<IllegalStateException> { bufferedSource.skip(1) }
    assertFailsWith<IllegalStateException> { bufferedSource.readByte() }
    assertFailsWith<IllegalStateException> { bufferedSource.exhausted() }
    assertFailsWith<IllegalStateException> { bufferedSource.require(1) }
    assertFailsWith<IllegalStateException> { bufferedSource.readByteArray() }
    assertFailsWith<IllegalStateException> { bufferedSource.peek() }
  }

  /**
   * We don't want transferTo to buffer an unbounded amount of data. Instead it
   * should buffer a segment, write it, and repeat.
   */
  @Test fun transferToReadsOneSegmentAtATime() {
    val write1 = Buffer().also { it.writeUtf8("a".repeat(Segment.SIZE)) }
    val write2 = Buffer().also { it.writeUtf8("b".repeat(Segment.SIZE)) }
    val write3 = Buffer().also { it.writeUtf8("c".repeat(Segment.SIZE)) }

    val source = Buffer()
    source.writeUtf8(
      "${"a".repeat(Segment.SIZE)}${"b".repeat(Segment.SIZE)}${"c".repeat(Segment.SIZE)}")

    val mockSink = MockSink()
    val bufferedSource = (source as RawSource).buffered()
    assertEquals(Segment.SIZE.toLong() * 3L, bufferedSource.transferTo(mockSink))
    mockSink.assertLog(
      "write($write1, ${write1.size})",
      "write($write2, ${write2.size})",
      "write($write3, ${write3.size})"
    )
  }

  @Test fun closeMultipleTimes() {
    var closeCalls = 0
    val rawSource: RawSource = object : RawSource {
      override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1
      override fun close() { closeCalls++ }
    }
    val source = rawSource.buffered()

    source.close()
    assertFailsWith<IllegalStateException> { source.readByte() }
    source.close() // should do nothing
    assertEquals(1, closeCalls)
  }

  @Test fun readAtMostFromEmptySource() {
    val rawSource = object : RawSource {
      override fun readAtMostTo(sink: Buffer, byteCount: Long): Long { return -1 }
      override fun close() {}
    }

    assertEquals(-1, rawSource.buffered().readAtMostTo(Buffer(), 1024))
  }

  @Test fun readAtMostFromFinite() {
    val rawSource = object : RawSource {
      var remainingBytes: Long = 10
      override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (remainingBytes == 0L) return -1
        val toWrite = minOf(remainingBytes, byteCount)
        remainingBytes -= toWrite
        sink.write(ByteArray(toWrite.toInt()))
        return toWrite
      }
      override fun close() {}
    }

    val source = rawSource.buffered()
    assertEquals(10, source.readAtMostTo(Buffer(), 1024))
    assertEquals(-1, source.readAtMostTo(Buffer(), 1024))
  }
}
