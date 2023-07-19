import Multiplatform_lib_conventions_gradle.IoMultiplatformExtension
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("multiplatform-lib-conventions")
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

kotlin {
    js(IR) {
        nodejs {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "30s"
                }
            }
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

extensions.configure<IoMultiplatformExtension> {
    javaVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
}
