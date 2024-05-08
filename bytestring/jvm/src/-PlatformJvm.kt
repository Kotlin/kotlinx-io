/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

// Incremental compilation doesn't work smoothly with OptionalExpectation (see KT-66317),
// so we have to explicitly actualize the annotation.
@Target(AnnotationTarget.FIELD)
internal actual annotation class BenignDataRace actual constructor()

