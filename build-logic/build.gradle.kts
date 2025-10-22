/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.power.assert.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.animalsniffer.gradle.plugin)
}

kotlin {
    jvmToolchain(JavaLanguageVersion.of(libs.versions.java.get()).asInt())
}
