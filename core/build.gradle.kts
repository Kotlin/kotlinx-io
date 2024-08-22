/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    alias(libs.plugins.kover)
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinx-io-bytestring"))
        }
        appleTest.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}


animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}
