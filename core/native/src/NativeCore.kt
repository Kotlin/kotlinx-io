/*
 * Copyright 2017-$today.yer JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.FileSink
import kotlinx.io.files.FileSource
import platform.posix.*

@OptIn(kotlinx. cinterop. ExperimentalForeignApi::class)
public actual fun stdinSource(): RawSource = FileSource(stdin!!)

@OptIn(kotlinx. cinterop. ExperimentalForeignApi::class)
public actual fun stdoutSink(): RawSink = FileSink(stdout!!)

@OptIn(kotlinx. cinterop. ExperimentalForeignApi::class)
public actual fun stderrSink(): RawSink = FileSink(stderr!!)
