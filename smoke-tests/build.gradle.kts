import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    kotlin("jvm")
}

val stagingRepositoryIdRawValue = project.findProperty("smokeTest.stagingRepository")?.toString()

val stagingRepositoryId: String = if (stagingRepositoryIdRawValue != null) {
    stagingRepositoryIdRawValue
} else {
    logger.warn("smokeTest.stagingRepository was not set.")
    ""
}

val stagingRepository: String = "https://oss.sonatype.org/content/repositories/$stagingRepositoryId"

val kotlinxIoVersionRawValue = project.findProperty("smokeTest.kotlinxIoVersion")?.toString()
var useLocalBuild = false

val kotlinxIoVersion: String = if (kotlinxIoVersionRawValue.isNullOrBlank()) {
    useLocalBuild = true
    val v = version.toString()
    logger.warn("smokeTest.kotlinxIoVersion was not specified, using $v instead.")
    v
} else {
    kotlinxIoVersionRawValue
}

repositories {
    mavenCentral()
}

tasks {
    val kotlinVersion: String = libs.versions.kotlin.get()

    val verifyMavenProjects by registering(Exec::class) {
        workingDir = projectDir.resolve("src").resolve("test").resolve("resources").resolve("maven-projects")
        executable = workingDir.resolve(getMavenWrapperName()).absolutePath
        args = buildList {
            add("-DKOTLIN_VERSION=$kotlinVersion")
            add("-DKOTLINX_IO_VERSION=$kotlinxIoVersion")
            if (stagingRepository.isNotBlank()) {
                add("-DSTAGING_REPOSITORY_URL=$stagingRepository")
                add("-Pstaging")
            }
            add("verify")
        }
        if (useLocalBuild) {
            dependsOn(project(":kotlinx-io-core").tasks.named("publishToMavenLocal"))
            dependsOn(project(":kotlinx-io-bytestring").tasks.named("publishToMavenLocal"))
        }
    }
    val cleanMavenProjects by registering(Exec::class) {
        workingDir = projectDir.resolve("src").resolve("test").resolve("resources").resolve("maven-projects")
        executable = workingDir.resolve(getMavenWrapperName()).absolutePath
        args = listOf("-DKOTLIN_VERSION=$kotlinVersion", "-DKOTLINX_IO_VERSION=$kotlinxIoVersion", "clean")
    }

    val verifyGradleProjects = create("verifyGradleProjects", Test::class) {
        useJUnit()
        if (useLocalBuild) {
            dependsOn(project(":kotlinx-io-core").tasks.named("publishToMavenLocal"))
            dependsOn(project(":kotlinx-io-bytestring").tasks.named("publishToMavenLocal"))
        }

        systemProperty("kotlinxIoVersion", kotlinxIoVersion)
        systemProperty("stagingRepository", stagingRepository)
        systemProperty("useLocalBuild", useLocalBuild)
        systemProperty("kotlinVersion", kotlinVersion)
    }

    create("smokeTest") {
        dependsOn(verifyMavenProjects)
        dependsOn(verifyGradleProjects)
    }

    named("clean").configure {
        dependsOn(cleanMavenProjects)
    }

    check.configure {
        enabled = false
    }
    test.configure {
        enabled = false
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

fun getMavenWrapperName(): String =
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        "mvnw.cmd"
    } else {
        "mvnw"
    }
