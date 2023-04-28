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

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path as NioPath
import java.security.MessageDigest
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.Mac

/** Returns a sink that writes to `out`. */
fun OutputStream.sink(): RawSink = OutputStreamSink(this)

private open class OutputStreamSink(
  private val out: OutputStream,
) : RawSink {

  override fun write(source: Buffer, byteCount: Long) {
    checkOffsetAndCount(source.size, 0, byteCount)
    var remaining = byteCount
    while (remaining > 0) {
      // kotlinx.io TODO: detect Interruption.
      val head = source.head!!
      val toCopy = minOf(remaining, head.limit - head.pos).toInt()
      out.write(head.data, head.pos, toCopy)

      head.pos += toCopy
      remaining -= toCopy
      source.size -= toCopy

      if (head.pos == head.limit) {
        source.head = head.pop()
        SegmentPool.recycle(head)
      }
    }
  }

  override fun flush() = out.flush()

  override fun close() = out.close()

  override fun cancel() {
    // Not cancelable.
  }

  override fun toString() = "sink($out)"
}

/** Returns a source that reads from `in`. */
fun InputStream.source(): RawSource = InputStreamSource(this)

private open class InputStreamSource(
  private val input: InputStream,
) : RawSource {

  override fun read(sink: Buffer, byteCount: Long): Long {
    if (byteCount == 0L) return 0L
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    try {
      val tail = sink.writableSegment(1)
      val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit).toInt()
      val bytesRead = input.read(tail.data, tail.limit, maxToCopy)
      if (bytesRead == -1) {
        if (tail.pos == tail.limit) {
          // We allocated a tail segment, but didn't end up needing it. Recycle!
          sink.head = tail.pop()
          SegmentPool.recycle(tail)
        }
        return -1
      }
      tail.limit += bytesRead
      sink.size += bytesRead
      return bytesRead.toLong()
    } catch (e: AssertionError) {
      if (e.isAndroidGetsocknameError) throw IOException(e)
      throw e
    }
  }

  override fun close() = input.close()

  override fun cancel() {
    // Not cancelable.
  }

  override fun toString() = "source($input)"
}

/**
 * Returns a sink that writes to `socket`. Prefer this over [sink]
 * because this method honors timeouts. When the socket
 * write times out, the socket is asynchronously closed by a watchdog thread.
 */
@Throws(IOException::class)
fun Socket.sink(): RawSink {
  return object : OutputStreamSink(getOutputStream()) {
    override fun cancel() {
      this@sink.close()
    }
  }
}

/**
 * Returns a source that reads from `socket`. Prefer this over [source]
 * because this method honors timeouts. When the socket
 * read times out, the socket is asynchronously closed by a watchdog thread.
 */
@Throws(IOException::class)
fun Socket.source(): RawSource {
  return object : InputStreamSource(getInputStream()) {
    override fun cancel() {
      this@source.close()
    }
  }
}

/** Returns a sink that writes to `file`. */
fun File.sink(append: Boolean = false): RawSink = FileOutputStream(this, append).sink()

/** Returns a sink that writes to `file`. */
fun File.appendingSink(): RawSink = FileOutputStream(this, true).sink()

/** Returns a source that reads from `file`. */
fun File.source(): RawSource = InputStreamSource(inputStream())

/** Returns a source that reads from `path`. */
fun NioPath.sink(vararg options: OpenOption): RawSink =
  Files.newOutputStream(this, *options).sink()

/** Returns a sink that writes to `path`. */
fun NioPath.source(vararg options: OpenOption): RawSource =
  Files.newInputStream(this, *options).source()

/**
 * Returns true if this error is due to a firmware bug fixed after Android 4.2.2.
 * https://code.google.com/p/android/issues/detail?id=54072
 */
internal val AssertionError.isAndroidGetsocknameError: Boolean get() {
  return cause != null && message?.contains("getsockname failed") ?: false
}
