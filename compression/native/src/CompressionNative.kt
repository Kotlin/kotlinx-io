/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

/**
 * Native implementation of [CompressionException].
 */
public actual open class CompressionException : kotlinx.io.IOException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(cause: Throwable?) : super(cause)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}
