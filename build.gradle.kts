/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("org.jetbrains.dokka") version "1.8.20"
    `maven-publish`
    signing
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

allprojects {
    properties["DeployVersion"]?.let { version = it }
    repositories {
        mavenCentral()
    }
}

apply(plugin = "maven-publish")
apply(plugin = "signing")

subprojects {
    if (name.contains("benchmark")) {
        return@subprojects
    }

    repositories {
        mavenCentral()
    }

    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    publishing {
        repositories {
            configureMavenPublication(project)
        }

        val javadocJar = project.configureEmptyJavadocArtifact()
        publications.withType(MavenPublication::class).all {
            pom.configureMavenCentralMetadata(project)
            signPublicationIfKeyPresent(project, this)
            artifact(javadocJar)
        }
    }
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_1_8.toString()
            allWarningsAsErrors = true
        }
    }
    tasks.withType<KotlinNativeCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
        }
    }
}

apiValidation {
    ignoredProjects.add("kotlinx-io-benchmarks")
}