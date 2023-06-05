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
 * Returns a new source that buffers reads from the source. The returned source will perform bulk
 * reads into its in-memory buffer. Use this wherever you read a source to get ergonomic and
 * efficient access to data.
 */
fun RawSource.buffer(): Source = RealSource(this)

/**
 * Returns a new sink that buffers writes to the sink. The returned sink will batch writes to the sink.
 * Use this wherever you write to a sink to get ergonomic and efficient access to data.
 */
fun RawSink.buffer(): Sink = RealSink(this)

/**
 * Returns a sink that writes nowhere.
 */
fun blackholeSink(): RawSink = BlackholeSink()

private class BlackholeSink : RawSink {
  override fun write(source: Buffer, byteCount: Long) = source.skip(byteCount)
  override fun flush() {}
  override fun cancel() {}
  override fun close() {}
}

/**
 * Execute [block] then close this. This will be closed even if [block] throws.
 */
inline fun <T : Closeable?, R> T.use(block: (T) -> R): R {
  var result: R? = null
  var thrown: Throwable? = null

  try {
    result = block(this)
  } catch (t: Throwable) {
    thrown = t
  }

  try {
    this?.close()
  } catch (t: Throwable) {
    if (thrown == null) thrown = t
    else thrown.addSuppressed(t)
  }

  if (thrown != null) throw thrown
  return result!!
}
