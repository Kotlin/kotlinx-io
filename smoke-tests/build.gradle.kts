import org.apache.tools.ant.taskdefs.condition.Os

apply(plugin = "kotlin")

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

val kotlinxIoVersion: String = if (kotlinxIoVersionRawValue != null) {
    kotlinxIoVersionRawValue
} else {
    useLocalBuild = true
    val v = version.toString()
    logger.warn("smokeTest.kotlinxIoVersion was not specified, using $v instead.")
    v
}

repositories {
    mavenCentral()
    if (stagingRepository.isNotBlank()) {
        maven(stagingRepository)
    }
    if (useLocalBuild) {
        mavenLocal()
    }
}

subprojects {
    repositories {
        mavenCentral()
        if (stagingRepository.isNotBlank()) {
            maven(stagingRepository)
        }
        if (useLocalBuild) {
            mavenLocal()
        }
    }
}

tasks {
    val kotlinVersion: String = libs.versions.kotlin.get()

    val verifyMavenProjects by registering(Exec::class) {
        workingDir = File(projectDir, "maven-projects")
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
    }
    val cleanMavenProjects by registering(Exec::class) {
        workingDir = File(projectDir, "maven-projects")
        executable = workingDir.resolve(getMavenWrapperName()).absolutePath
        args = listOf("-DKOTLIN_VERSION=$kotlinVersion", "-DKOTLINX_IO_VERSION=$kotlinxIoVersion", "clean")
    }
    val smokeTest = create("smokeTest") {
        dependsOn(verifyMavenProjects)
    }

    subprojects {
        afterEvaluate {
            tasks.named("smokeTest").configure {
                smokeTest.dependsOn(this)
            }
        }
    }

    named("clean").configure {
        dependsOn(cleanMavenProjects)
    }
}

fun getMavenWrapperName(): String =
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        "mvnw.cmd"
    } else {
        "mvnw"
    }
