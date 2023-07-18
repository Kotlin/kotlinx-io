/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    alias(libs.plugins.bcv)
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
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
            freeCompilerArgs += "-Xjvm-default=all"
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
