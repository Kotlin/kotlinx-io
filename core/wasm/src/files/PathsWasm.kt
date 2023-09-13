/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

public actual class Path private constructor() {
    actual override fun toString(): String = unsupported()

    public actual val parent: Path?
        get() = unsupported()

    public actual val name: String
        get() = unsupported()


    public actual val isAbsolute: Boolean
        get() = unsupported()
}

public actual val SystemPathSeparator: Char
    get() = unsupported()

public actual fun Path(path: String): Path = unsupported()
