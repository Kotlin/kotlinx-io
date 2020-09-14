pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://dl.bintray.com/orangy/maven")
        maven("https://dl.bintray.com/kotlin/kotlinx")
        maven("https://dl.bintray.com/mipt-npm/dev")
        mavenLocal()
    }

    val kotlin_version: String by settings
    val benchmarks_version: String by settings

    plugins{
        kotlin("multiplatform") version kotlin_version
        kotlin("plugin.allopen") version kotlin_version
        id("kotlinx.benchmark") version benchmarks_version
        id("com.vanniktech.android.junit.jacoco") version "0.15.0"
        id("ru.mipt.npm.publish") version "0.6.0"
    }
}

rootProject.name = "kotlinx-io-package"

include(":core")
project(":core").name = "kotlinx-io"

include(":benchmarks")
project(":benchmarks").name = "kotlinx-io-benchmarks"

include(":playground")
project(":playground").name = "kotlinx-io-playground"
