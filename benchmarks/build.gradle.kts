/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.benchmark.gradle.JvmBenchmarkTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinx.benchmark.plugin)
    id("kotlinx-io-clean")
}

kotlin {
    jvm {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-io-core"))
                implementation(libs.kotlinx.benchmark.runtime)
            }
        }

        named("jvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation("io.github.fzhinkin:xctraceprof:0.0.3")
            }
        }
    }
}

val nativeBenchmarksEnabled: String by project.parent!!

if (nativeBenchmarksEnabled.toBoolean()) {
    kotlin {
        // TODO: consider supporting non-host native targets.
        if (HostManager.host === KonanTarget.MACOS_X64) macosX64("native")
        if (HostManager.host === KonanTarget.MACOS_ARM64) macosArm64("native")
        if (HostManager.hostIsLinux) linuxX64("native")
        if (HostManager.hostIsMingw) mingwX64("native")

        sourceSets {
            named("nativeMain") {
                dependsOn(commonMain.get())
            }
        }
    }
}

benchmark {
    targets {
        register("jvm") {
            this as JvmBenchmarkTarget
            jmhVersion = libs.versions.jmh.get()
        }
        if (nativeBenchmarksEnabled.toBoolean()) {
            register("native")
        }
    }
}
