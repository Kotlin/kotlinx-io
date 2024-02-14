/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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

    js(IR) {
        browser {
            testTask(Action {
                filter.setExcludePatterns("*SmokeFileTest*")
            })
        }
    }

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        //  Disabled because we can't exclude some tests: https://youtrack.jetbrains.com/issue/KT-58291
        // browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }

    explicitApi()
    sourceSets.configureEach {
        configureSourceSet()
    }

    configureNativePlatforms()

    val appleTargets = appleTargets()
    val mingwTargets = mingwTargets()

    /*
      Native source sets hierarchy:
      native
      |-> apple
      |-> nonApple
          |-> mingw
          |-> unix
              |-> linux
              |-> android
     */

    sourceSets {
        val nativeMain = createSourceSet("nativeMain", parent = commonMain.get())
        val nativeTest = createSourceSet("nativeTest", parent = commonTest.get())
        val nonAppleMain = createSourceSet("nonAppleMain", parent = nativeMain)
        val nonAppleTest = createSourceSet("nonAppleTest", parent = nativeTest)
        createSourceSet("appleMain", parent = nativeMain, children = appleTargets)
        createSourceSet("appleTest", parent = nativeTest, children = appleTargets)
        createSourceSet("mingwMain", parent = nonAppleMain, children = mingwTargets)
        createSourceSet("mingwTest", parent = nonAppleTest, children = mingwTargets)
        val unixMain = createSourceSet("unixMain", parent = nonAppleMain)
        val unixTest = createSourceSet("unixTest", parent = nonAppleTest)
        createSourceSet("linuxMain", parent = unixMain, children = linuxTargets())
        createSourceSet("linuxTest", parent = unixTest, children = linuxTargets())
        createSourceSet("androidMain", parent = unixMain, children = androidTargets())
        createSourceSet("androidTest", parent = unixTest, children = androidTargets())
        createSourceSet("nodeFilesystemSharedMain", parent = commonMain.get(), children = nodeTargets())
        createSourceSet("nodeFilesystemSharedTest", parent = commonTest.get(), children = nodeTargets())
        createSourceSet("wasmMain", parent = commonMain.get(), children = wasmTargets())
        createSourceSet("wasmTest", parent = commonTest.get(), children = wasmTargets())
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

/**
 * Creates a source set for a directory that isn't already a built-in platform. Use this to create
 * custom shared directories like `nonJvmMain` or `unixMain`.
 */
fun NamedDomainObjectContainer<KotlinSourceSet>.createSourceSet(
    name: String,
    parent: KotlinSourceSet? = null,
    children: List<String> = listOf()
): KotlinSourceSet {
    val result = create(name)

    if (parent != null) {
        result.dependsOn(parent)
    }

    val suffix = when {
        name.endsWith("Main") -> "Main"
        name.endsWith("Test") -> "Test"
        else -> error("unexpected source set name: ${name}")
    }

    for (childTarget in children) {
        val childSourceSet = get("${childTarget}$suffix")
        childSourceSet.dependsOn(result)
    }

    return result
}

fun KotlinMultiplatformExtension.configureNativePlatforms() {
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
    linuxArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()
}

fun appleTargets() = listOf(
    "iosArm64",
    "iosX64",
    "iosSimulatorArm64",
    "macosX64",
    "macosArm64",
    "tvosArm64",
    "tvosX64",
    "tvosSimulatorArm64",
    "watchosArm32",
    "watchosArm64",
    "watchosX64",
    "watchosSimulatorArm64",
    "watchosDeviceArm64"
)

fun mingwTargets() = listOf(
    "mingwX64"
)

fun linuxTargets() = listOf(
    "linuxX64",
    "linuxArm64"
)

fun androidTargets() = listOf(
    "androidNativeArm32",
    "androidNativeArm64",
    "androidNativeX64",
    "androidNativeX86"
)

fun wasmTargets() = listOf("wasmJs", "wasmWasi")

fun nodeTargets() = listOf("js", "wasmJs")

rootProject.the<NodeJsRootExtension>().apply {
    nodeVersion = "21.0.0-v8-canary202310177990572111"
    nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
}

rootProject.tasks.withType<KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
}
