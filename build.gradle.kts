/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.validation.ExperimentalBCVApi

plugins {
    id("kotlinx-io-publish") apply false

    alias(libs.plugins.kover)
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)

    alias(libs.plugins.android) apply false
    alias(libs.plugins.androidx.benchmark) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    properties["DeployVersion"]?.let { version = it }
    repositories {
        mavenCentral()
    }
}

@OptIn(ExperimentalBCVApi::class)
apiValidation {
    ignoredProjects.add("kotlinx-io-benchmarks")
    val androidBenchmarksEnabled = project.findProperty("androidBenchmarksEnabled")?.toString()?.toBoolean() ?: false
    if (androidBenchmarksEnabled) {
        ignoredProjects.add("kotlinx-io-benchmarks-android")
    }
    klib.enabled = true
}

dependencies {
    kover(project(":kotlinx-io-core"))
    kover(project(":kotlinx-io-bytestring"))
}

kover {
    reports {
        verify {
            rule {
                minBound(95, CoverageUnit.LINE)

                // we allow lower branch coverage, because not all checks in the internal code lead to errors
                minBound(80, CoverageUnit.BRANCH)
            }
        }
    }
}
