/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

internal fun unsupported(): Nothing = TODO("Paths are not supported for Wasm target")

internal actual val SystemTemporaryDirectoryImpl: Path
    get() = unsupported()

internal actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {

    override fun exists(path: Path): Boolean = unsupported()

    override fun delete(path: Path, mustExist: Boolean) = unsupported()

    override fun createDirectories(path: Path, mustCreate: Boolean) = unsupported()

    override fun atomicMove(source: Path, destination: Path) = unsupported()

    override fun read(path: Path): Source = unsupported()

    override fun write(path: Path): Sink = unsupported()

    override fun metadataOrNull(path: Path): FileMetadata = unsupported()

}

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)
