/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.7.0")
}

rootProject.name = "kotlinx-io"

include(":kotlinx-io-core")
include(":kotlinx-io-benchmarks")
include(":kotlinx-io-bytestring")
project(":kotlinx-io-core").projectDir = file("./core")
project(":kotlinx-io-benchmarks").projectDir = file("./benchmarks")
project(":kotlinx-io-bytestring").projectDir = file("./bytestring")
