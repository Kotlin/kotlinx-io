import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    id("kotlinx-io-dokka")
    id("kotlinx-io-android-compat")
    alias(libs.plugins.kover)
}

kotlin {
    js(IR) {
        nodejs {
            testTask(Action {
                useMocha {
                    timeout = "30s"
                }
            })
        }
        browser {
            testTask(Action {
                useMocha {
                    timeout = "30s"
                }
            })
        }
    }
}


tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        perPackageOption {
            suppress.set(true)
            matchingRegex.set(".*unsafe.*")
        }

        samples.from("common/test/samples/samples.kt")
    }
}
