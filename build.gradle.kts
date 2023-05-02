/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.11.1"
    `maven-publish`
    signing
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
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
        }
    }
}
