/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("multiplatform")
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true

        klib {
            enabled = true
        }
    }
}

tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}
