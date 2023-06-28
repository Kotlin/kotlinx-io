/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover") version "0.7.1"
    id("org.jetbrains.dokka") version "1.8.20"
}

kotlin {
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js(IR) {
        nodejs {
            testTask {
                useMocha {
                    timeout = "300s"
                }
            }
        }
        browser {
            testTask {
                filter.setExcludePatterns("*SmokeFileTest*")
                useMocha {
                    timeout = "300s"
                }
            }
        }
    }

    configureNativePlatforms()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":kotlinx-io-bytestring"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting

        createSourceSet("nativeMain", parent = commonMain, children = nativeTargets)
        createSourceSet("nativeTest", parent = commonTest, children = nativeTargets)
    }

    explicitApi()
    sourceSets.configureEach {
        configureSourceSet()
    }
}

fun KotlinSourceSet.configureSourceSet() {
    val srcDir = if (name.endsWith("Main")) "src" else "test"
    val platform = name.dropLast(4)
    kotlin.srcDir("$platform/$srcDir")
    if (name == "jvmMain") {
        resources.srcDir("$platform/resources")
    } else if (name == "jvmTest") {
        resources.srcDir("$platform/test-resources")
    }
    languageSettings {
        progressiveMode = true
    }
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}