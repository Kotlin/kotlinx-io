/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import kotlin.jvm.optionals.getOrNull

plugins {
    kotlin("multiplatform")
    id("kotlinx-io-clean")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        allWarningsAsErrors = true
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    val versionCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    jvmToolchain {
        val javaVersion = versionCatalog.findVersion("java").getOrNull()?.requiredVersion
            ?: throw GradleException("Version 'java' is not specified in the version catalog")
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        // can be replaced with just `compilerOptions { }` in Kotlin 2.0
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xjvm-default=all")
                }
            }
        }
    }

    js {
        browser {
            testTask {
                filter.setExcludePatterns("*SmokeFileTest*")
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        //  Disabled because we can't exclude some tests: https://youtrack.jetbrains.com/issue/KT-58291
        // browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    nativeTargets()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    explicitApi()
    sourceSets.configureEach {
        configureSourceSet()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("native") {
                group("nativeNonApple") {
                    group("mingw")
                    group("unix") {
                        group("linux")
                        group("androidNative")
                    }
                }

                group("nativeNonAndroid") {
                    group("apple")
                    group("mingw")
                    group("linux")
                }
            }
            group("nodeFilesystemShared") {
                withJs()
                withWasmJs()
            }
            group("wasm") {
                withWasmJs()
                withWasmWasi()
            }
        }
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

private fun KotlinMultiplatformExtension.nativeTargets() {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()

    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    watchosDeviceArm64()

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    linuxX64()
    linuxArm64()
    @Suppress("DEPRECATION") // https://github.com/Kotlin/kotlinx-io/issues/303
    linuxArm32Hfp()

    macosX64()
    macosArm64()

    mingwX64()
}
