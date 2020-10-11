@file:Suppress("UNUSED_VARIABLE")

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
    jacoco
    id("org.jetbrains.dokka")
}

kotlin {
    explicitApi()
    jvm()
    js { nodejs { testTask { debug = false } } }
    val hostOs = System.getProperty("os.name")
    val nativeTargets = mutableListOf<KotlinNativeTarget>()

    nativeTargets += when {
        hostOs == "Mac OS X" -> macosX64()
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        commonTest.get().dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
        }

        val jvmTest by getting { dependencies { api(kotlin("test-junit")) } }
        val jsTest by getting { dependencies { api(kotlin("test-js")) } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeMain) } }
            val test by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeTest) } }
        }

        all {
            val isTest = toString().endsWith("Test")
            kotlin.srcDir("$name/src")
            resources.srcDir("$name/resources")

            languageSettings.apply {
                progressiveMode = true
                enableLanguageFeature("InlineClasses")
                useExperimentalAnnotation("kotlin.Experimental")
                useExperimentalAnnotation("kotlinx.io.core.ExperimentalIoApi")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")

                if (isTest) {
                    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                }
            }
        }
    }

    configure(nativeTargets) {
        val main by compilations.getting {
            val bits by cinterops.creating { defFile = file("nativeMain/interop/bits.def") }
            val sockets by cinterops.creating { defFile = file("nativeMain/interop/sockets.def") }
        }

        val test by compilations.getting {
            val testSockets by cinterops.creating { defFile = file("nativeTest/interop/testSockets.def") }
        }
    }
}

internal val emptyJavadoc by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
}

tasks.dokkaGfm.get().outputDirectory.set(file("$buildDir/dokka"))

publishing {
    kotlin.targets.forEach { target ->
        publications
            .withType<MavenPublication>()
            .find { it.name == target.name }
            ?.artifact(emptyJavadoc)
    }

    val vcs = System.getenv("VCS_URL")

    publications.withType<MavenPublication>().forEach { publication ->
        publication.pom {
            name.set(project.name)
            description.set(project.description)
            url.set(vcs)

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
        }
    }

    val bintrayUser = System.getenv("BINTRAY_USER")
    val bintrayKey = System.getenv("BINTRAY_KEY")

    if (bintrayUser != null && bintrayKey != null) repositories.maven {
        name = "bintray"
        url = uri("https://api.bintray.com/maven/commandertvis/kotlinx-io/kotlinx-io/;publish=1;override=1")

        this.credentials {
            username = bintrayUser
            password = bintrayKey
        }
    }
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("${buildDir}/jacoco-reports")
}
