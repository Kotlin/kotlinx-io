/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial

buildscript {
    dependencies {
        classpath(libs.kotlinx.atomicfu.plugin)
    }
}

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

apply(plugin = "kotlinx-atomicfu")
dependencies {
    testImplementation(project(mapOf("path" to ":kotlinx-io-core")))
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-io-core"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        create("concurrentMain") {
            dependsOn(getByName("commonMain"))
            getByName("jvmMain").dependsOn(this)
            getByName("nativeMain").dependsOn(this)
        }

        create("concurrentTest") {
            dependsOn(getByName("commonTest"))
            getByName("jvmTest").dependsOn(this)
            getByName("nativeTest").dependsOn(this)
        }
    }

    js(IR) {
        nodejs {
            testTask(Action {
                useMocha {
                    timeout = "30s"
                }
            })
        }
        browser {
            testTask(Action {
                useMocha {
                    timeout = "30s"
                }
            })
        }
    }
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        samples.from(project.fileTree("common/test/samples/"))
    }
}
