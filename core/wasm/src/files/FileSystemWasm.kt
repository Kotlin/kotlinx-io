/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource

internal fun unsupported(): Nothing = TODO("Paths are not supported for Wasm target")

public actual val SystemTemporaryDirectory: Path
    get() = unsupported()

public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {

    override fun exists(path: Path): Boolean = unsupported()

    override fun delete(path: Path, mustExist: Boolean) = unsupported()

    override fun createDirectories(path: Path, mustCreate: Boolean) = unsupported()

    override fun atomicMove(source: Path, destination: Path) = unsupported()

    override fun source(path: Path): RawSource = unsupported()

    override fun sink(path: Path, append: Boolean): RawSink = unsupported()

    override fun metadataOrNull(path: Path): FileMetadata = unsupported()

}

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)
