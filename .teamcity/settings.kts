import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.*
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.*

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2018.2"
val versionSuffixParameter = "versionSuffix"
val teamcitySuffixParameter = "teamcitySuffix"
val releaseVersionParameter = "releaseVersion"
val bintrayUserName = "orangy"
val bintrayToken = "credentialsJSON:9a48193c-d16d-46c7-8751-2fb434b09e07"
val platforms = listOf("Windows", "Linux", "Mac OS X")

project {
    // Disable editing of project and build settings from the UI to avoid issues with TeamCity
    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildAll = buildAll()
    val builds = platforms.map { build(it) }

    builds.forEach { build ->
        buildAll.dependsOn(build) {
            snapshot {
                onDependencyFailure = FailureAction.ADD_PROBLEM
                onDependencyCancel = FailureAction.CANCEL
            }
        }

        buildAll.dependsOn(build) {
            artifacts {
                artifactRules = "+:maven=>maven"
            }
        }
    }

    val deployConfigure = deployConfigure()
    val deploys = platforms.map { deploy(it, deployConfigure) }

    val deployPublish = deployPublish(deployConfigure).apply {
        deploys.forEach {
            dependsOnSnapshot(it)
        }
    }

    buildTypesOrder = listOf(buildAll) + builds + deployPublish + deployConfigure + deploys
}

fun Project.buildAll() = BuildType {
    id("Build_All")
    this.name = "Build (All)"
    type = BuildTypeSettings.Type.COMPOSITE

    triggers {
        vcs {
            triggerRules = """
                    -:*.md
                    -:.gitignore
                """.trimIndent()
        }
    }

    commonConfigure()
}.also { buildType(it) }

fun Project.build(platform: String) = platform(platform, "Build") {
    steps {
        gradle {
            param("env.LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
            name = "Build and Test $platform Binaries"
            jdkHome = "%env.JDK_18_x64%"
            jvmArgs = "-Xmx1g"
            tasks = "clean check"
            // --continue is needed to run tests for all targets even if one target fails
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=SNAPSHOT -P$teamcitySuffixParameter=%build.counter% --continue"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }

    // What files to publish as build artifacts
    artifactRules = "+:build/maven=>maven"
}

fun BuildType.dependsOn(build: BuildType, configure: Dependency.() -> Unit) =
    apply {
        dependencies.dependency(build, configure)
    }

fun BuildType.dependsOnSnapshot(build: BuildType, configure: SnapshotDependency.() -> Unit = {}) = apply {
    dependencies.dependency(build) {
        snapshot {
            configure()
            onDependencyFailure = FailureAction.FAIL_TO_START
            onDependencyCancel = FailureAction.CANCEL
        }
    }
}

fun Project.deployConfigure() = BuildType {
    id("Deploy_Configure")
    this.name = "Deploy (Configure)"
    commonConfigure()

    params {
        // enable editing of this configuration to set up things
        param("teamcity.ui.settings.readOnly", "false")
        param("bintray-user", bintrayUserName)
        password("bintray-key", bintrayToken)
        param(versionSuffixParameter, "dev-%build.counter%")
        // Intentionally left empty. Gradle will ignore empty values and in custom build it can be specified
        param(releaseVersionParameter, "dev")
    }

    requirements {
        // Require Linux for configuration build
        contains("teamcity.agent.jvm.os.name", "Linux")
    }

    steps {
        gradle {
            param("env.LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
            name = "Verify Gradle Configuration"
            tasks = "clean publishBintrayCreateVersion"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
            buildFile = ""
            jdkHome = "%env.JDK_18%"
        }
    }
}.also { buildType(it) }

fun Project.deployPublish(configureBuild: BuildType) = BuildType {
    id("Deploy_Publish")
    this.name = "Deploy (Publish)"
    type = BuildTypeSettings.Type.COMPOSITE

    params {
        param(versionSuffixParameter, "${configureBuild.depParamRefs[versionSuffixParameter]}")
        param(releaseVersionParameter, "${configureBuild.depParamRefs[releaseVersionParameter]}")
    }

    commonConfigure()
}.also { buildType(it) }.dependsOnSnapshot(configureBuild)

fun Project.deploy(platform: String, configureBuild: BuildType) = platform(platform, "Deploy") {
    type = BuildTypeSettings.Type.DEPLOYMENT
    enablePersonalBuilds = false
    maxRunningBuilds = 1
    params {
        param(versionSuffixParameter, "${configureBuild.depParamRefs[versionSuffixParameter]}")
        param(releaseVersionParameter, "${configureBuild.depParamRefs[releaseVersionParameter]}")
        param("bintray-user", bintrayUserName)
        password("bintray-key", bintrayToken)
    }

    vcs {
        cleanCheckout = true
    }

    steps {
        gradle {
            param("env.LIBCLANG_DISABLE_CRASH_RECOVERY", "1")
            name = "Deploy $platform Binaries"
            jdkHome = "%env.JDK_18_x64%"
            jvmArgs = "-Xmx1g"
            gradleParams = "--info --stacktrace -P$versionSuffixParameter=%$versionSuffixParameter% -P$releaseVersionParameter=%$releaseVersionParameter% -PbintrayApiKey=%bintray-key% -PbintrayUser=%bintray-user%"
            tasks = "clean build publish"
            buildFile = ""
            gradleWrapperPath = ""
        }
    }
}.dependsOnSnapshot(configureBuild)

fun Project.platform(platform: String, name: String, configure: BuildType.() -> Unit) = BuildType {
    // ID is prepended with Project ID, so don't repeat it here
    // ID should conform to identifier rules, so just letters, numbers and underscore
    id("${name}_${platform.substringBefore(" ")}")
    // Display name of the build configuration
    this.name = "$name ($platform)"

    requirements {
        contains("teamcity.agent.jvm.os.name", platform)
    }

    params {
        // This parameter is needed for macOS agent to be compatible
        param("env.JDK_17", "")
    }

    commonConfigure()
    configure()
}.also { buildType(it) }


fun BuildType.commonConfigure() {
    requirements {
        noLessThan("teamcity.agent.hardware.memorySizeMb", "6144")
    }

    // Allow to fetch build status through API for badges
    allowExternalStatus = true

    // Configure VCS, by default use the same and only VCS root from which this configuration is fetched
    vcs {
        root(DslContext.settingsRoot)
        showDependenciesChanges = true
        checkoutMode = CheckoutMode.ON_AGENT
    }

    failureConditions {
        errorMessage = true
        nonZeroExitCode = true
        executionTimeoutMin = 120
    }

    features {
        feature {
            id = "perfmon"
            type = "perfmon"
        }
    }
}
