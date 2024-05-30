/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

public actual open class IOException : Exception {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(cause: Throwable?) : super(cause)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class EOFException : IOException {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}

public actual open class UnknownServiceException : IOException {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
}
