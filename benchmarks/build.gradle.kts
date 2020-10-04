@file:Suppress("PropertyName", "UNUSED_VARIABLE")

val benchmarks_version: String by project

plugins {
    kotlin("multiplatform")
    kotlin("plugin.allopen")
    id("kotlinx.benchmark")
}

// how to apply plugin to a specific source set?
allOpen.annotation("org.openjdk.jmh.annotations.State")


kotlin {
//    infra {
//        target('macosX64')
//        target('linuxX64')
//        target('mingwX64')
//    }

    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
    }

// TODO: lookup benchmark configuration for js
//    js {
//        nodejs()
//    }

    sourceSets.all {
        kotlin.srcDir("$name/src")
        resources.srcDir("$name/resources")

        languageSettings.apply {
            progressiveMode = true
            useExperimentalAnnotation("kotlin.Experimental")
            useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            useExperimentalAnnotation("kotlinx.io.core.ExperimentalIoApi")
            useExperimentalAnnotation("kotlinx.io.unsafe.DangerousInternalIoApi")
            useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:$benchmarks_version")
                implementation(project(":kotlinx-io"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }

        val nativeMain by creating {}
    }
}

// Configure benchmark
benchmark {
    configurations {
        val main by getting {
            iterations = 1000 // number of iterations
            iterationTime = 1
            iterationTimeUnit = "s"
        }
    }

    // Setup configurations
    targets {
        // This one matches compilation base name, e.g. 'jvm', 'jvmTest', etc
        register("jvm") {}
        register("js") {}
        register("native") {}
    }
}