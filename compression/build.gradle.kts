/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    alias(libs.plugins.kover)
}

kotlin {
    // Compression is only available on JVM and Native (no JS/Wasm support)
    // JS and Wasm targets are excluded from the default hierarchy

    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinx-io-core"))
        }
    }
}

// Remove JS and Wasm targets as compression relies on platform-specific libraries
kotlin.targets.removeAll { target ->
    target.platformType.name == "js" || target.platformType.name == "wasm"
}
