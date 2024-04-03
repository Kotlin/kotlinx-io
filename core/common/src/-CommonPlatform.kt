/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

/**
 * Signals about a general issue occurred during I/O operation.
 */
public expect open class IOException : Exception {
    public constructor()
    public constructor(message: String?)
    public constructor(cause: Throwable?)
    public constructor(message: String?, cause: Throwable?)
}

/**
 * Signals that the end of the file or stream was reached unexpectedly during an input operation.
 */
public expect open class EOFException : IOException {
    public constructor()
    public constructor(message: String?)
}


// There is no actual AutoCloseable on JVM (https://youtrack.jetbrains.com/issue/KT-55777),
// but on JVM we have to explicitly implement by RawSink and the compiler does not allow that.
// This is a workaround that should be removed as soon as stdlib will support AutoCloseable
// actual typealias on JVM.
@OptIn(ExperimentalStdlibApi::class)
internal typealias AutoCloseableAlias = AutoCloseable
