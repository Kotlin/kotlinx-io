/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    alias(libs.plugins.kover)
}

kotlin {
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "300s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "300s"
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinx-io-bytestring"))
        }
        appleTest.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}
