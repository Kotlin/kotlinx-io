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
    repositories {
        mavenCentral()
    }
}

properties["DeployVersion"]?.let { version = it }
subprojects {
    repositories {
        mavenCentral()
    }
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    publishing {
        publications.withType(MavenPublication::class).all {
            pom.configureMavenCentralMetadata(project)
            configureEmptyJavadocArtifact(project)
            signPublicationIfKeyPresent(project, this)
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
