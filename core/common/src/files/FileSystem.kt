/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.Sink
import kotlinx.io.Source

public interface FileSystem {
    public val temporaryDirectory: Path

    public fun exists(path: Path): Boolean

    public fun delete(path: Path, mustExist: Boolean = true)

    public fun createDirectories(path: Path)

    public fun atomicMove(source: Path, destination: Path)

    public fun read(path: Path): Source = path.source()

    public fun write(path: Path): Sink = path.sink()

    public companion object {
        public val System: FileSystem = SystemFileSystem
    }
}

internal expect val SystemFileSystem: FileSystem
