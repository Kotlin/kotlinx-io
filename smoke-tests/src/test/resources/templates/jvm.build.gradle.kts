/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    kotlin("jvm") version "%KOTLIN_VERSION%"
}

val stagingRepository: String = "%STAGING_REPOSITORY%"
val useLocalRepo: Boolean = "%USE_LOCAL_REPO%".toBoolean()

repositories {
    mavenCentral()
    if (stagingRepository.isNotBlank()) {
        maven(url = stagingRepository)
    }
    if (useLocalRepo) {
        mavenLocal()
    }
}

dependencies {
    implementation("%DEPENDENCY%")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(8)
}
