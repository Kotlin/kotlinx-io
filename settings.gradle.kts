@file:Suppress("LocalVariableName")

pluginManagement {
    val benchmarks_version: String by settings
    val dokka_version: String by settings
    val kotlin_version: String by settings

    repositories {
        mavenCentral()
        jcenter()
        gradlePluginPortal()
        maven(url = "https://dl.bintray.com/kotlin/kotlinx")
    }

    plugins {
        kotlin(module = "multiplatform") version kotlin_version
        kotlin(module = "plugin.allopen") version kotlin_version
        id("kotlinx.benchmark") version benchmarks_version
        id("org.jetbrains.dokka") version dokka_version
    }
}

rootProject.name = "kotlinx-io-package"
include(":core")
project(":core").name = "kotlinx-io"
include(":benchmarks")
project(":benchmarks").name = "kotlinx-io-benchmarks"
include(":playground")
project(":playground").name = "kotlinx-io-playground"
