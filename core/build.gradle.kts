/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

kotlin {
    js(IR) {
        nodejs {
            testTask(Action {
                useMocha {
                    timeout = "300s"
                }
            })
        }
        browser {
            testTask(Action {
                useMocha {
                    timeout = "300s"
                }
            })
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-io-bytestring"))
            }
        }
        appleTest {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
            }
        }
    }
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        samples.from(
            "common/test/samples/rawSinkSample.kt",
            "common/test/samples/rawSourceSample.kt",
            "common/test/samples/moduleDescriptionSample.kt",
            "common/test/samples/samples.kt",
            "common/test/samples/byteStringSample.kt",
            "jvm/test/samples/samplesJvm.kt",
            "apple/test/samples/samplesApple.kt"
        )
    }
}
