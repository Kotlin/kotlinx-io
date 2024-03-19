/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import kotlin.jvm.optionals.getOrNull

plugins {
    kotlin("multiplatform")
    id("kotlinx-io-clean")
}

kotlin {

    val versionCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    jvmToolchain {
        val javaVersion = versionCatalog.findVersion("java").getOrNull()?.requiredVersion
            ?: throw GradleException("Version 'java' is not specified in the version catalog")
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }

    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    js {
        browser {
            testTask(Action {
                filter.setExcludePatterns("*SmokeFileTest*")
            })
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
                group("nonApple") {
                    group("mingw")
                    group("unix") {
                        group("linux")
                        group("androidNative")
                    }
                }

                group("nonAndroid") {
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

// will be available in KGP 2.0
@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.withWasmJs(): Unit = withCompilations {
    val target = it.target
    target.platformType == KotlinPlatformType.wasm &&
            target is KotlinJsIrTarget &&
            target.wasmTargetType == KotlinWasmTargetType.JS
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinHierarchyBuilder.withWasmWasi(): Unit = withCompilations {
    val target = it.target
    target.platformType == KotlinPlatformType.wasm &&
            target is KotlinJsIrTarget &&
            target.wasmTargetType == KotlinWasmTargetType.WASI
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

rootProject.the<NodeJsRootExtension>().apply {
    nodeVersion = "21.0.0-v8-canary202310177990572111"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
}

rootProject.tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
