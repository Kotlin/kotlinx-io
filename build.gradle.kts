/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import kotlinx.validation.ExperimentalBCVApi
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("kotlinx-io-publish") apply false

    alias(libs.plugins.kover)
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)
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

@OptIn(ExperimentalBCVApi::class)
apiValidation {
    ignoredProjects.add("kotlinx-io-benchmarks")
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
                // TODO: rollback to 95
            // minBound(94, CoverageUnit.LINE)

                // we allow lower branch coverage, because not all checks in the internal code lead to errors
                // minBound(80, CoverageUnit.BRANCH)
            }
        }
    }
}
