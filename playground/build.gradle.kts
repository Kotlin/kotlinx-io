@file:Suppress("UNUSED_VARIABLE")

plugins { kotlin("multiplatform") }

kotlin {
    jvm()

    sourceSets.all {
        kotlin.srcDir("${name}/src")
        resources.srcDir("${name}/resources")
        languageSettings.progressiveMode = true
    }
}

kotlin.sourceSets {
    val jvmMain by getting {
        dependencies {
            api(project(":kotlinx-io"))
        }
    }
}
