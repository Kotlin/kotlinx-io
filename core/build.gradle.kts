/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    alias(libs.plugins.kover)
}

kotlin {
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "300s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "300s"
                }
            }
        }
    }
    wasmWasi {
        nodejs {
            testTask {
                // fd_readdir is unsupported on Windows:
                // https://github.com/nodejs/node/blob/6f4d6011ea1b448cf21f5d363c44e4a4c56ca34c/deps/uvwasi/src/uvwasi.c#L19
                if (OperatingSystem.current().isWindows) {
                    filter.setExcludePatterns("*SmokeFileTest.listDirectory")
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinx-io-bytestring"))
        }
        appleTest.dependencies {
            implementation(libs.kotlinx.coroutines.core)
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
            "apple/test/samples/samplesApple.kt",
            "jvm/test/samples/unsafe/unsafeAccessSamples.kt"
        )
    }
}

tasks.named("wasmWasiNodeTest") {
    // TODO: remove once https://youtrack.jetbrains.com/issue/KT-65179 solved
    doFirst {
        val layout = project.layout
        val templateFile = layout.projectDirectory.file("wasmWasi/test/test-driver.mjs.template").asFile

        val driverFile = layout.buildDirectory.file(
            "compileSync/wasmWasi/test/testDevelopmentExecutable/kotlin/kotlinx-io-kotlinx-io-core-wasm-wasi-test.mjs"
        )

        fun File.mkdirsAndEscape(): String {
            mkdirs()
            return absolutePath.replace("\\", "\\\\")
        }

        val tmpDir = temporaryDir.resolve("kotlinx-io-core-wasi-test").mkdirsAndEscape()
        val tmpDir2 = temporaryDir.resolve("kotlinx-io-core-wasi-test-2").mkdirsAndEscape()

        val newDriver = templateFile.readText()
            .replace("<SYSTEM_TEMP_DIR>", tmpDir, false)
            .replace("<SYSTEM_TEMP_DIR2>", tmpDir2, false)

        driverFile.get().asFile.writeText(newDriver)
    }
}

animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}
