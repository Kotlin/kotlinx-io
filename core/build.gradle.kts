import org.jetbrains.kotlin.gradle.plugin.*

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    configureNativePlatforms()
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting

        createSourceSet("nativeMain", parent = commonMain, children = nativeTargets)
        createSourceSet("nativeTest", parent = commonTest, children = nativeTargets)
    }

    // TODO quite a lot of effort, should be done after the initial set of API is migrated
//    explicitApi()
    sourceSets.configureEach {
        configureSourceSet()
    }
}

fun KotlinSourceSet.configureSourceSet() {
    val srcDir = if (name.endsWith("Main")) "src" else "test"
    val platform = name.dropLast(4)
    kotlin.srcDir("$platform/$srcDir")
    if (name == "jvmMain") {
        resources.srcDir("$platform/resources")
    } else if (name == "jvmTest") {
        resources.srcDir("$platform/test-resources")
    }
    languageSettings {
        progressiveMode = true
    }
}
