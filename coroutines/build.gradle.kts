/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

plugins {
    kotlin("multiplatform")
//    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-compatibility")
    alias(libs.plugins.kover)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kotlinx-io-core"))
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }

    jvm()

    explicitApi()

    withSourcesJar()

//    js {
//        nodejs {
//            testTask {
//                useMocha {
//                    timeout = "300s"
//                }
//            }
//        }
//        browser {
//            testTask {
//                useMocha {
//                    timeout = "300s"
//                }
//            }
//        }
//    }
}
