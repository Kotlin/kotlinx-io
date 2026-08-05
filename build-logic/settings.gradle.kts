/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("file:///Users/Nikolay.Lunyak/Documents/Projects/kotlin-worktrees/kotlin-platform-type-commonized-to-different-types/build/repo")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
