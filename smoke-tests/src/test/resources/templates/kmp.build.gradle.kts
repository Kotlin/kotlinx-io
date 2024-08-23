/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.gradle.api.Project

plugins {
    kotlin("multiplatform") version "%KOTLIN_VERSION%"
}

val stagingRepository: String = "%STAGING_REPOSITORY%"
val useLocalRepo: Boolean = "%USE_LOCAL_REPO%".toBoolean()

repositories {
    mavenCentral()
    if (stagingRepository.isNotBlank()) {
        maven(url = stagingRepository)
    }
    if (useLocalRepo) {
        mavenLocal()
    }
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
        commonMain {
            dependencies {
                implementation("%DEPENDENCY%")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
