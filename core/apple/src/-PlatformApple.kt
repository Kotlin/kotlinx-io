/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

/**
 * Sequence of characters used as a line separator by the underlying platform.
 *
 * The value of this property is always `"\n"`.
 */
public actual val SystemLineSeparator: String = "\n"
