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

private const val SEGMENT_SIZE = Segment.SIZE

class BufferCommonTest {
  @Test fun completeSegmentByteCountOnEmptyBuffer() {
    val buffer = Buffer()
    assertEquals(0, buffer.completeSegmentByteCount())
  }

  @Test fun completeSegmentByteCountOnBufferWithFullSegments() {
    val buffer = Buffer()
    buffer.writeUtf8("a".repeat(Segment.SIZE * 4))
    assertEquals((Segment.SIZE * 4).toLong(), buffer.completeSegmentByteCount())
  }

  @Test fun completeSegmentByteCountOnBufferWithIncompleteTailSegment() {
    val buffer = Buffer()
    buffer.writeUtf8("a".repeat(Segment.SIZE * 4 - 10))
    assertEquals((Segment.SIZE * 3).toLong(), buffer.completeSegmentByteCount())
  }

  @Test
  fun cloneDoesNotObserveWritesToOriginal() {
    val original = Buffer()
    val clone: Buffer = original.copy()
    original.writeUtf8("abc")
    assertEquals(0, clone.size)
  }

  @Test
  fun cloneDoesNotObserveReadsFromOriginal() {
    val original = Buffer()
    original.writeUtf8("abc")
    val clone: Buffer = original.copy()
    assertEquals("abc", original.readUtf8(3))
    assertEquals(3, clone.size)
    assertEquals("ab", clone.readUtf8(2))
  }

  @Test
  fun originalDoesNotObserveWritesToClone() {
    val original = Buffer()
    val clone: Buffer = original.copy()
    clone.writeUtf8("abc")
    assertEquals(0, original.size)
  }

  @Test
  fun originalDoesNotObserveReadsFromClone() {
    val original = Buffer()
    original.writeUtf8("abc")
    val clone: Buffer = original.copy()
    assertEquals("abc", clone.readUtf8(3))
    assertEquals(3, original.size)
    assertEquals("ab", original.readUtf8(2))
  }

  @Test
  fun cloneMultipleSegments() {
    val original = Buffer()
    original.writeUtf8("a".repeat(SEGMENT_SIZE * 3))
    val clone: Buffer = original.copy()
    original.writeUtf8("b".repeat(SEGMENT_SIZE * 3))
    clone.writeUtf8("c".repeat(SEGMENT_SIZE * 3))

    assertEquals("a".repeat(SEGMENT_SIZE * 3) + "b".repeat(SEGMENT_SIZE * 3),
      original.readUtf8((SEGMENT_SIZE * 6).toLong()))
    assertEquals("a".repeat( SEGMENT_SIZE * 3) + "c".repeat(SEGMENT_SIZE * 3),
      clone.readUtf8((SEGMENT_SIZE * 6).toLong()))
  }
}
