/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("kotlinx-io-publish") apply false
    id("kotlinx-io-dokka")
    alias(libs.plugins.kover)
}

allprojects {
    properties["DeployVersion"]?.let { version = it }
    repositories {
        mavenCentral()
        maven("file:///Users/Nikolay.Lunyak/Documents/Projects/kotlin-worktrees/kotlin-platform-type-commonized-to-different-types/build/repo")
    }
}

dependencies {
    kover(project(":kotlinx-io-core"))
    kover(project(":kotlinx-io-bytestring"))
    kover(project(":kotlinx-io-okio"))

    dokka(project(":kotlinx-io-bytestring"))
    dokka(project(":kotlinx-io-core"))
    dokka(project(":kotlinx-io-okio"))
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

allprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        println("Project: $name")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.configureEach {
                dependencies {
                    implementation("org.jetbrains.kotlin.commonizer:commonizer-support-library:2.4.255-SNAPSHOT")
                }
            }

            compilerOptions.freeCompilerArgs.add("-Xskip-prerelease-check")
        }
    }
}