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
 * Receives a stream of bytes. RawSink is a base interface for `kotlinx-io` data receivers.
 *
 * This interface should be implemented to write data wherever it's needed: to the network, storage,
 * or a buffer in memory. Sinks may be layered to transform received data, such as to compress, encrypt, throttle,
 * or add protocol framing.
 *
 * Most application code shouldn't operate on a raw sink directly, but rather on a buffered [Sink] which
 * is both more efficient and more convenient. Use [buffer] to wrap any raw sink with a buffer.
 *
 * Implementors should abstain from throwing exceptions other than those that are documented for RawSink methods.
 */
@OptIn(ExperimentalStdlibApi::class)
public expect interface RawSink : AutoCloseableAlias {
  /**
   * Removes [byteCount] bytes from [source] and appends them to this sink.
   *
   * @param source the source to read data from.
   * @param byteCount the number of bytes to write.
   *
   * @throws IllegalArgumentException when the [source]'s size is below [byteCount] or [byteCount] is negative.
   * @throws IllegalStateException when the sink is closed.
   */
  public fun write(source: Buffer, byteCount: Long)

  /**
   * Pushes all buffered bytes to their final destination.
   *
   * @throws IllegalStateException when the sink is closed.
   */
  public fun flush()

  /**
   * Pushes all buffered bytes to their final destination and releases the resources held by this
   * sink. It is an error to write a closed sink. It is safe to close a sink more than once.
   */
  override fun close()
}
