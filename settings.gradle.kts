pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "kotlinx-io"

include(":kotlinx-io-core")
project(":kotlinx-io-core").projectDir = file("./core")
