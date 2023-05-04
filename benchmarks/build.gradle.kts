/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.7"
}

kotlin {
    jvm()
    macosArm64("native")

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlinx-io-core"))
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.7")
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