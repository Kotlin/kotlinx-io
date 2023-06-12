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

/**
 * Supplies a stream of bytes. RawSource is a base interface for `kotlinx-io` data suppliers.
 *
 * The interface should be implemented to read data from wherever it's located: from the network, storage,
 * or a buffer in memory. Sources may be layered to transform supplied data, such as to decompress, decrypt,
 * or remove protocol framing.
 *
 * Most applications shouldn't operate on a raw source directly, but rather on a buffered [Source] which
 * is both more efficient and more convenient. Use [buffer] to wrap any raw source with a buffer.
 */
public interface RawSource : Closeable {
  /**
   * Removes at least 1, and up to [byteCount] bytes from this source and appends them to [sink].
   * Returns the number of bytes read, or -1 if this source is exhausted.
   *
   * @param sink the destination to write the data from this source.
   * @param byteCount the number of bytes to read.
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   */
  public fun read(sink: Buffer, byteCount: Long): Long

  /**
   * Closes this source and releases the resources held by this source. It is an error to read a
   * closed source. It is safe to close a source more than once.
   */
  override fun close()
}
