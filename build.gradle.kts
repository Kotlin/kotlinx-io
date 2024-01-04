/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

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

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs += "-Xjvm-default=all"
            freeCompilerArgs += "-Xexpect-actual-classes"
        }
    }
    tasks.withType<KotlinNativeCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs += "-Xexpect-actual-classes"
        }
    }
    tasks.withType<KotlinJsCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs += "-Xexpect-actual-classes"
        }
    }
}

apiValidation {
    ignoredProjects.add("kotlinx-io-benchmarks")
    val androidBenchmarksEnabled = project.findProperty("androidBenchmarksEnabled")?.toString()?.toBoolean() ?: false
    if (androidBenchmarksEnabled) {
        ignoredProjects.add("kotlinx-io-benchmarks-android")
    }
}

dependencies {
    kover(project(":kotlinx-io-core"))
    kover(project(":kotlinx-io-bytestring"))
}

koverReport {
    verify {
        rule {
            // TODO: rollback to 95
            // minBound(94, MetricType.LINE)

            // we allow lower branch coverage, because not all checks in the internal code lead to errors
            // minBound(80, MetricType.BRANCH)
        }
    }
}
