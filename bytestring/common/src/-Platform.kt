/*
* Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
*/
package kotlinx.io.bytestring

/**
 * Annotation indicating that the marked property is the subjectof benign data race.
 * LLVM does not support this notion, so on K/N platforms we alias it into `@Volatile` to prevent potential OoTA.
 */
@OptionalExpectation
@Target(AnnotationTarget.FIELD)
@OptIn(ExperimentalMultiplatform::class)
internal expect annotation class BenignDataRace()
