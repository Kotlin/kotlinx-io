/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.8"
}

kotlin {
    jvm()
    // TODO: consider supporting non-host native targets.
    if (HostManager.host === KonanTarget.MACOS_X64) macosX64("native")
    if (HostManager.host === KonanTarget.MACOS_ARM64) macosArm64("native")
    if (HostManager.hostIsLinux) linuxX64("native")
    if (HostManager.hostIsMingw) mingwX64("native")

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-io-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.8")
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain.get())
        }

        val nativeMain by getting {
            dependsOn(commonMain.get())
        }
    }
}

benchmark {
    targets {
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.36"
        }
        register("native")
    }
}
