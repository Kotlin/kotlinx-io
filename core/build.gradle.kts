/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinTargetWithNodeJsDsl

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    alias(libs.plugins.kover)
}

repositories {
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
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

    fun KotlinTargetWithNodeJsDsl.filterSmokeTests() {
        this.nodejs {
            testTask(Action {
                useMocha {
                    timeout = "300s"
                }
                filter.setExcludePatterns("*SmokeFileTest*")
            })
        }
    }

//    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
//    wasmJs {
//        filterSmokeTests()
//    }
//
//    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
//    wasmWasi {
//        filterSmokeTests()
//    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-io-bytestring"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC-gradle-space2")
            }
        }
        appleTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
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

animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}
