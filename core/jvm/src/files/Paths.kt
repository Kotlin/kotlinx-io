/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.*
import java.nio.file.Path as NioPath

public actual class Path internal constructor(internal val nioPath: NioPath)

public actual fun Path(path: String): Path = Path(Paths.get(path))

public actual fun Path.source(): Source = FileInputStream(nioPath.toFile()).asSource().buffered()

public actual fun Path.sink(): Sink = FileOutputStream(nioPath.toFile()).asSink().buffered()
