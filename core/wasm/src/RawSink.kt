/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

public actual interface RawSink : AutoCloseable {
    public actual fun write(source: Buffer, byteCount: Long)

    public actual fun flush()

    actual override fun close()
}
