/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.sourceSets
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs {
            testTask {
                // fd_readdir is unsupported on Windows:
                // https://github.com/nodejs/node/blob/6f4d6011ea1b448cf21f5d363c44e4a4c56ca34c/deps/uvwasi/src/uvwasi.c#L19
                if (OperatingSystem.current().isWindows) {
                    filter.setExcludePatterns("*SmokeFileTest.listDirectory")
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

tasks.named("wasmWasiNodeTest") {
    // TODO: remove once https://youtrack.jetbrains.com/issue/KT-65179 solved
    doFirst {
        val layout = project.layout
        val templateFile = layout.projectDirectory.file("wasmWasi/test/test-driver.mjs.template").asFile

        val driverFile = layout.buildDirectory.file(
            "compileSync/wasmWasi/test/testDevelopmentExecutable/kotlin/kotlinx-io-kotlinx-io-core-wasm-wasi-test.mjs"
        )

        fun File.mkdirsAndEscape(): String {
            mkdirs()
            return absolutePath.replace("\\", "\\\\")
        }

        val tmpDir = temporaryDir.resolve("kotlinx-io-core-wasi-test").mkdirsAndEscape()
        val tmpDir2 = temporaryDir.resolve("kotlinx-io-core-wasi-test-2").mkdirsAndEscape()

        val newDriver = templateFile.readText()
            .replace("<SYSTEM_TEMP_DIR>", tmpDir, false)
            .replace("<SYSTEM_TEMP_DIR2>", tmpDir2, false)

        driverFile.get().asFile.writeText(newDriver)
    }
}

animalsniffer {
    annotation = "kotlinx.io.files.AnimalSnifferIgnore"
}

val mrjToolchain = libs.versions.multi.release.toolchain.getOrNull()
    ?: throw GradleException("Version 'multi.release.toolchain' is not specified in the version catalog")

private fun KotlinJvmCompilation.setupJava9CompileTasks() {
    compileTaskProvider.configure {
        this as KotlinJvmCompile

        kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(mrjToolchain))
        })

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_9)
            // TODO: don't use -Xjdk-release as it cause miscompilation, see KT-72880
            //freeCompilerArgs.add("-Xjdk-release=9")
        }
    }

    compileJavaTaskProvider?.configure {
        targetCompatibility = "9"
        sourceCompatibility = "9"
    }
}

kotlin {
    jvm {
        compilations {
            // it has to be <something>Main, see KotlinSourceSet.configureSourceSet
            val jvm9Main by creating {
                associateWith(getByName("main"))

                defaultSourceSet {
                    kotlin.srcDir("jvm/src9")
                }

                setupJava9CompileTasks()
            }

            create("jvm9Test") {
                associateWith(jvm9Main)
                associateWith(getByName("test"))

                defaultSourceSet {
                    kotlin.srcDir("jvm/test9")

                    dependencies {
                        implementation(kotlin("test-junit5"))
                    }

                }

                setupJava9CompileTasks()

                val jvm9TestTask = tasks.register<Test>("jvm9Test") {
                    group = "verification"

                    classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs
                    testClassesDirs = output.classesDirs

                    javaLauncher.set(javaToolchains.launcherFor {
                        languageVersion.set(JavaLanguageVersion.of(mrjToolchain))
                    })

                    useJUnitPlatform()
                }
                tasks.named("allTests").configure {
                    dependsOn(jvm9TestTask)
                }
            }

            tasks.withType(Jar::class.java).configureEach {
                from(jvm9Main.output) {
                    into("META-INF/versions/9")
                }
            }
        }
    }
}

kover {
    currentProject {
        instrumentation {
            // TODO: collect coverage for all tasks and remove jvm9Main from excluded srcsets
            disabledForTestTasks.add("jvm9Test")
        }
        sources {
            excludedSourceSets.addAll("jvm9Main", "jvm9Test")
        }
    }
}
