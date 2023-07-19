/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("publish-conventions") apply false

    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)
}

allprojects {
    properties["DeployVersion"]?.let { version = it }
    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs += "-Xjvm-default=all"
        }
    }
    tasks.withType<KotlinNativeCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
        }
    }
}

apiValidation {
    ignoredProjects.add("kotlinx-io-benchmarks")
}
