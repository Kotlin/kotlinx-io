import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.gradle.api.Project

/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    kotlin("multiplatform")
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    jvm()
    js(IR) {
        browser {
            testTask {
                useMocha()
            }
        }
        nodejs()
    }
    wasmWasi {
        nodejs()
    }
    wasmJs {
        nodejs()
        browser {
            testTask {
                useMocha()
            }
        }
    }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    watchosX64()
    watchosArm32()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    iosSimulatorArm64()
    watchosSimulatorArm64()

    linuxArm64()
    linuxX64()

    @Suppress("DEPRECATION")
    linuxArm32Hfp()

    macosArm64()
    macosX64()

    mingwX64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks {
    check.configure {
        enabled = false
    }

    create("smokeTest") {
        dependsOn(tasks.named("allTests"))
    }
}