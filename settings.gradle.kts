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
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

rootProject.name = "kotlinx-io"

include(":kotlinx-io-core")
include(":kotlinx-io-benchmarks")
include(":kotlinx-io-bytestring")
include(":kotlinx-io-smoke-tests")
include(":kotlinx-io-smoke-tests:gradle-bytestring-jvm")
include(":kotlinx-io-smoke-tests:gradle-bytestring-multiplatform")
include(":kotlinx-io-smoke-tests:gradle-core-jvm")
include(":kotlinx-io-smoke-tests:gradle-core-multiplatform")

project(":kotlinx-io-core").projectDir = file("./core")
project(":kotlinx-io-benchmarks").projectDir = file("./benchmarks")
project(":kotlinx-io-bytestring").projectDir = file("./bytestring")
project(":kotlinx-io-smoke-tests").projectDir = file("./smoke-tests")
project(":kotlinx-io-smoke-tests:gradle-bytestring-jvm").projectDir = file("./smoke-tests/gradle-projects/bytestring-jvm")
project(":kotlinx-io-smoke-tests:gradle-bytestring-multiplatform").projectDir = file("./smoke-tests/gradle-projects/bytestring-multiplatform")
project(":kotlinx-io-smoke-tests:gradle-core-jvm").projectDir = file("./smoke-tests/gradle-projects/core-jvm")
project(":kotlinx-io-smoke-tests:gradle-core-multiplatform").projectDir = file("./smoke-tests/gradle-projects/core-multiplatform")