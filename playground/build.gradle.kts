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
            api("com.squareup.okio:okio:1.0.0")
        }
    }
}
