import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("kotlinx-io-multiplatform")
    id("kotlinx-io-publish")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
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
        includes.from("Module.md")

        perPackageOption {
            suppress.set(true)
            matchingRegex.set(".*unsafe.*")
        }

        samples.from("common/test/samples/samples.kt")
    }
}
