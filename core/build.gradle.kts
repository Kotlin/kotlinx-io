import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
    jacoco
    id("org.jetbrains.dokka")
}

kotlin {
    explicitApi()
    jvm()
    js { nodejs { testTask { debug = false } } }
    val hostOs = System.getProperty("os.name")
    val nativeTargets = mutableListOf<KotlinNativeTarget>()

    when {
        hostOs == "Mac OS X" -> {
            nativeTargets += macosX64()
            nativeTargets += iosX64()
            nativeTargets += iosArm64()
            nativeTargets += iosArm32()
        }

        hostOs == "Linux" -> nativeTargets += linuxX64()
        hostOs.startsWith("Windows") -> nativeTargets += mingwX64()
        else -> throw GradleException("Host OS '$hostOs' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        commonTest.get().dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
        }

        val jvmTest by getting { dependencies { api(kotlin("test-junit")) } }
        val jsTest by getting { dependencies { api(kotlin("test-js")) } }
        val nativeMain by creating { dependsOn(commonMain.get()) }
        val nativeTest by creating { dependsOn(commonTest.get()) }

        configure(nativeTargets) {
            val main by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeMain) } }
            val test by compilations.getting { kotlinSourceSets.forEach { it.dependsOn(nativeTest) } }
        }

        all {
            val isTest = toString().endsWith("Test")
            kotlin.srcDir("$name/src")
            resources.srcDir("$name/resources")

            languageSettings.apply {
                progressiveMode = true
                enableLanguageFeature("InlineClasses")
                useExperimentalAnnotation("kotlin.Experimental")
                useExperimentalAnnotation("kotlinx.io.core.ExperimentalIoApi")
                useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")

                if (isTest) {
                    useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
                    useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
                }
            }
        }
    }

    configure(nativeTargets) {
        val main by compilations.getting {
            val bits by cinterops.creating { defFile = file("nativeMain/interop/bits.def") }
            val sockets by cinterops.creating { defFile = file("nativeMain/interop/sockets.def") }
        }

        val test by compilations.getting {
            val testSockets by cinterops.creating { defFile = file("nativeTest/interop/testSockets.def") }
        }
    }

    val emptyJavadoc = task<Jar>("emptyJavadoc") {
        classifier = "javadoc"
    }

    targets.forEach { target ->
        publishing
            .publications
            .withType<MavenPublication>()
            .find { it.name == target.name }?.artifact(emptyJavadoc)
    }
}

tasks.dokkaGfm.get().outputDirectory = "$buildDir/dokka"

publishing {
    val vcs = System.getenv("VCS_URL")

    // Process each publication we have in this project
    publications.withType<MavenPublication>().forEach { publication ->
        publication.pom {
            name.set(project.name)
            description.set(project.description)
            url.set(vcs)

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
        }
    }

    val bintrayOrg = System.getenv("BINTRAY_ORG")
    val bintrayUser = System.getenv("BINTRAY_USER")
    val bintrayKey = System.getenv("BINTRAY_KEY")
    val bintrayRepo = System.getenv("BINTRAY_REPO")
    val projectName = project.name

    if (bintrayRepo != null && bintrayUser != null && bintrayKey != null) {
        project.logger.info("Adding bintray publishing to project [$projectName]")

        repositories.maven {
            name = "bintray"
            url = uri("https://api.bintray.com/maven/$bintrayOrg/$bintrayRepo/$projectName/")

            credentials {
                username = bintrayUser
                password = bintrayKey
            }
        }
    }
}

//tasks.jacoco {
//    toolVersion = "0.8.5"
//    reportsDir = file("${buildDir}/jacoco-reports")
//}
//
//task<JacocoReport>("testCoverage") {
//    dependsOn(tasks.jvmTest)
//    group = "Reporting"
//    description = "Generate Jacoco coverage reports."
//
//    val coverageSourceDirs = listOf(
//        "commonMain/src",
//        "jvmMain/src"
//    )
//
////    classDirectories.from files (fileTree(dir = "${buildDir}/classes/kotlin/jvm/"))
////    sourceDirectories.from files (coverageSourceDirs)
////    additionalSourceDirs.from files (coverageSourceDirs)
////    executionData.from files ("${buildDir}/jacoco/jvmTest.exec")
//
//    reports {
//        //TODO
////        xml.enabled = false
////        csv.enabled = false
////        html.enabled = true
//
////        html.destination file ("${buildDir}/jacoco-reports/html")
//    }
//}
//
//// Workaround: register and publish the cinterop klibs as outputs of intermediate source sets:
//
//if (!false) {
//    apply from : "$rootDir/gradle/interop-as-source-set-klib.gradle"
//
//    kotlin.sourceSets {
//        bitsInterop
//        socketsInterop
//        nativeMain {
//            dependsOn(bitsInterop)
//            dependsOn(socketsInterop)
//        }
//
//        def linuxInterops = kotlin . linuxX64 ().compilations["main"].cinterops
//
//        registerInteropAsSourceSetOutput(linuxInterops.bits, bitsInterop)
//        registerInteropAsSourceSetOutput(linuxInterops.sockets, socketsInterop)
//    }
//}