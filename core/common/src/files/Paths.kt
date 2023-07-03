/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * A wrapper around a string representing a file path allowing to read from and write to a
 * corresponding file using [Sink] and [Source].
 *
 * **This API is unstable and subject to change.**
 */
public expect class Path

/**
 * Returns Path for the given string without much of a validation.
 */
public expect fun Path(path: String): Path

/**
 * Returns [Source] for the given file or throws if path is not a file or does not exist
 */
public expect fun Path.source(): Source

/**
 * Returns [Sink] for the given path, creates file if it doesn't exist, throws if it's a directory,
 * overwrites contents.
 */
public expect fun Path.sink(): Sink
