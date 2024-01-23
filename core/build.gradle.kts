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

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmWasi {
        //filterSmokeTests()
    }

    sourceSets {
        commonMain {
            dependencies {
                api(project(":kotlinx-io-bytestring"))
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

val replaceWasiNodeTestDriver by tasks.creating {
    dependsOn("compileTestDevelopmentExecutableKotlinWasmWasi")
    val layout = project.layout
    val templateFile = layout.projectDirectory.dir("wasmWasi")
        .dir("test")
        .file("test-driver.mjs.template")
        .asFile

    val fileName = "kotlinx-io-kotlinx-io-core-wasm-wasi-test.mjs"
    val driverFile = layout.buildDirectory.map {
        it.dir("compileSync")
            .dir("wasmWasi")
            .dir("test")
            .dir("testDevelopmentExecutable")
            .dir("kotlin")
            .file(fileName)
    }

    doLast {
        val tmpDir = File(System.getProperty("java.io.tmpdir"))//, "kotlinx-io-core-wasi-test")
            .also { it.mkdirs() }
            .absolutePath

        val newDriver = templateFile.readText().replace("<SYSTEM_TEMP_DIR>", tmpDir, false)

        driverFile.get().asFile.writeText(newDriver)
    }
}

tasks.named("wasmWasiNodeTest").configure {
    dependsOn(replaceWasiNodeTestDriver)
}

animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}
