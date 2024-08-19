/*
 * Copyright 2017-$today.yer JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.FileSink
import kotlinx.io.files.FileSource

public actual fun stdinSource(): RawSource = FileSource(0)
public actual fun stdoutSink(): RawSink = FileSink(1, false)
public actual fun stderrSink(): RawSink = FileSink(2, false)
