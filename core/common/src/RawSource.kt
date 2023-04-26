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
 * Supplies a stream of bytes. Use this interface to read data from wherever it's located: from the
 * network, storage, or a buffer in memory. Sources may be layered to transform supplied data, such
 * as to decompress, decrypt, or remove protocol framing.
 *
 * Most applications shouldn't operate on a source directly, but rather on a [Source] which
 * is both more efficient and more convenient. Use [buffer] to wrap any source with a buffer.
 *
 * Sources are easy to test: just use a [Buffer] in your tests, and fill it with the data your
 * application is to read.
 *
 * ### Comparison with InputStream

 * This interface is functionally equivalent to [java.io.InputStream].
 *
 * `InputStream` requires multiple layers when consumed data is heterogeneous: a `DataInputStream`
 * for primitive values, a `BufferedInputStream` for buffering, and `InputStreamReader` for strings.
 * This library uses `BufferedSource` for all of the above.
 *
 * RawSource avoids the impossible-to-implement [available()][java.io.InputStream.available] method.
 * Instead callers specify how many bytes they [require][Source.require].
 *
 * RawSource omits the unsafe-to-compose [mark and reset][java.io.InputStream.mark] state that's
 * tracked by `InputStream`; instead, callers just buffer what they need.
 *
 * When implementing a source, you don't need to worry about the [read()][java.io.InputStream.read]
 * method that is awkward to implement efficiently and returns one of 257 possible values.
 *
 * And source has a stronger `skip` method: [Source.skip] won't return prematurely.
 *
 * ### Interop with InputStream
 *
 * Use [source] to adapt an `InputStream` to a source. Use [Source.inputStream] to adapt a
 * source to an `InputStream`.
 */
interface RawSource : Closeable {
  /**
   * Removes at least 1, and up to `byteCount` bytes from this and appends them to `sink`. Returns
   * the number of bytes read, or -1 if this source is exhausted.
   */
  @Throws(IOException::class)
  fun read(sink: Buffer, byteCount: Long): Long

  /**
   * Asynchronously cancel this source. Any [read] in flight should immediately fail with an
   * [IOException], and any future read should also immediately fail with an [IOException].
   *
   * Note that it is always necessary to call [close] on a source, even if it has been canceled.
   */
  fun cancel()

  /**
   * Closes this source and releases the resources held by this source. It is an error to read a
   * closed source. It is safe to close a source more than once.
   */
  @Throws(IOException::class)
  override fun close()
}
