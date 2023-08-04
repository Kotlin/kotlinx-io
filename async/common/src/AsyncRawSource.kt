/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer

@OptIn(ExperimentalStdlibApi::class)
public interface AsyncRawSource : AutoCloseable {
    public suspend fun readAtMostTo(buffer: Buffer, bytesCount: Long): Long
    override fun close()
}
