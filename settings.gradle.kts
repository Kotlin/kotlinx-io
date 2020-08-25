@file:Suppress("LocalVariableName")

pluginManagement {
    val kotlin_version: String by settings

    repositories {
        mavenCentral()
        jcenter()
        gradlePluginPortal()
        maven(url = "https://dl.bintray.com/orangy/maven")
        maven(url = "https://dl.bintray.com/kotlin/kotlinx")
    }

    plugins {
        id("org.jetbrains.dokka") version "1.4.0-rc"
        kotlin(module = "plugin.allopen") version kotlin_version
        kotlin(module = "multiplatform") version kotlin_version
    }
}

enableFeaturePreview("GRADLE_METADATA")
rootProject.name = "kotlinx-io-package"
include(":core")
project(":core").name = "kotlinx-io"
include(":benchmarks")
project(":benchmarks").name = "kotlinx-io-benchmarks"
include(":playground")
project(":playground").name = "kotlinx-io-playground"
