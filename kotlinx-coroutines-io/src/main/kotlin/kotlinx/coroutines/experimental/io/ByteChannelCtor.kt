/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental.io

/**
 * Channel for asynchronous reading and writing of sequences of bytes.
 * This is a buffered **single-reader single-writer channel**.
 *
 * Read operations can be invoked concurrently with write operations, but multiple reads or multiple writes
 * cannot be invoked concurrently with themselves. Exceptions are [close] and [flush] which can be invoked
 * concurrently with any other operations and between themselves at any time.
 */
interface ByteChannel : ByteReadChannel, ByteWriteChannel

/**
 * Creates buffered channel for asynchronous reading and writing of sequences of bytes.
 */
expect fun ByteChannel(autoFlush: Boolean = false): ByteChannel


/**
 * Creates channel for reading from the specified byte array.
 */
expect fun ByteReadChannel(content: ByteArray): ByteReadChannel


/**
 * Byte channel that is always empty.
 */
val EmptyByteReadChannel: ByteReadChannel = ByteReadChannel(ByteArray(0))

