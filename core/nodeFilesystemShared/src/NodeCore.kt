/*
 * Copyright 2017-$today.yer JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.FileSink
import kotlinx.io.files.FileSource

public actual fun stdinSource(): RawSource {
    var out: RawSource? = null
    withCaughtException {
        out = FileSource(0)
    }?.also {
        // TODO: it doesn't work
        out = object : RawSource {
            private var closed = false
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                if (closed) throw IOException("Source is closed.")
                return -1L
            }

            override fun close() {
                closed = true
            }
        }
    }

    return out!!
}

public actual fun stdoutSink(): RawSink {
    var sink: RawSink? = null
    withCaughtException {
        sink = FileSink(1)
    }?.also {
        // TODO: it doesn't work
        throw UnsupportedOperationException("Writing to stdout is not supported.", it)
    }
    return sink!!
}

public actual fun stderrSink(): RawSink {
    var sink: RawSink? = null
    withCaughtException {
        sink =  FileSink(2)
    }?.also {
        // TODO: it doesn't work
        throw UnsupportedOperationException("Writing to stderr is not supported.", it)
    }
    return sink!!
}
