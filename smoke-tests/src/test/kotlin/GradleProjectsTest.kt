/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals

private const val buildScriptFilename = "build.gradle.kts"
private const val settingsFilename = "settings.gradle.kts"

public class GradleProjectsTest {
    @JvmField
    @Rule
    public val projectDir = TemporaryFolder()

    private val kotlinxIoVersion: String = System.getProperty("kotlinxIoVersion")!!
    private val kotlinVersion: String = System.getProperty("kotlinVersion")!!
    private val useLocalBuild: String = System.getProperty("useLocalBuild")!!
    private val stagingRepository: String = System.getProperty("stagingRepository")!!
    private val bytestringDependency: String = "org.jetbrains.kotlinx:kotlinx-io-bytestring:$kotlinxIoVersion"
    private val coreDependency: String = "org.jetbrains.kotlinx:kotlinx-io-core:$kotlinxIoVersion"
    private val fsDependency: String = "org.jetbrains.kotlinx:kotlinx-io-filesystem:$kotlinxIoVersion"

    private val okioAdapterDependency: String = "org.jetbrains.kotlinx:kotlinx-io-okio:$kotlinxIoVersion"

    private fun generateBuildScript(multiplatform: Boolean, dependencyName: String, isOkio: Boolean = false) {
        val templateFile = (if (multiplatform) (if (isOkio) "kmp.okio" else "kmp") else "jvm") + "." + buildScriptFilename
        var template = GradleProjectsTest::class.java.getResourceAsStream(
            "/templates/$templateFile")!!.reader().readText()

        template = template.replace("%DEPENDENCY%", dependencyName)
            .replace("%KOTLIN_VERSION%", kotlinVersion)
            .replace("%USE_LOCAL_REPO%", useLocalBuild)
            .replace("%STAGING_REPOSITORY%", stagingRepository)

        val outFile = projectDir.newFile(buildScriptFilename)
        outFile.writeText(template)
    }

    private fun setupTest(testCase: String, multiplatform: Boolean, dependencyName: String) {
        copySrcFile(testCase, multiplatform)

        projectDir.newFile(settingsFilename).outputStream().use {
            GradleProjectsTest::class.java.getResourceAsStream("/templates/$settingsFilename")!!.copyTo(it)
        }

        generateBuildScript(multiplatform, dependencyName)
    }

    private fun setupOkioKmpTest() {
        // TODO: merge jvm and multiplatform test sources
        copySrcFile("okio", true)

        projectDir.newFile(settingsFilename).outputStream().use {
            GradleProjectsTest::class.java.getResourceAsStream("/templates/$settingsFilename")!!.copyTo(it)
        }

        generateBuildScript(true, okioAdapterDependency, isOkio = true)
    }

    private fun copySrcFile(testCase: String, multiplatform: Boolean) {
        val testSubdir = if (multiplatform) "commonTest" else "test"
        val srcDir = projectDir.newFolder("src", testSubdir, "kotlin")
        val resource = GradleProjectsTest::class.java.getResourceAsStream("/gradle-projects/$testCase/SmokeTest.kt")!!
        val outFile = srcDir.toPath().resolve("SmokeTest.kt")
        outFile.outputStream().use {
            resource.copyTo(it)
        }
    }

    private fun assertTestPassed(buildResult: BuildResult, taskName: String = ":test") {
        assertEquals(TaskOutcome.SUCCESS, buildResult.task(taskName)?.outcome,
            "Task \"$taskName\" should pass.\n" + buildResult.output)
    }

    @Test
    fun bytestringJvm() {
        setupTest("bytestring-jvm", false, bytestringDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":test")
            .run()

        assertTestPassed(results)
    }

    @Test
    fun coreJvm() {
        setupTest("core-jvm", false, coreDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":test")
            .run()

        assertTestPassed(results)
    }

    @Test
    fun filesystemJvm() {
        setupTest("filesystem-jvm", false, fsDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":test")
            .run()

        assertTestPassed(results)
    }

    @Test
    fun bytestringMultiplatform() {
        setupTest("bytestring-multiplatform", true, bytestringDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":allTests")
            .run()

        assertTestPassed(results, ":allTests")
    }

    @Test
    fun coreMultiplatform() {
        setupTest("core-multiplatform", true, coreDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":allTests")
            .run()

        assertTestPassed(results, ":allTests")
    }

    @Test
    fun filesystemMultiplatform() {
        setupTest("filesystem-multiplatform", true, fsDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":allTests")
            .run()

        assertTestPassed(results, ":allTests")
    }

    @Test
    fun okioJvm() {
        setupTest("okio", false, okioAdapterDependency)
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":test")
            .run()

        assertTestPassed(results)
    }

    @Test
    fun okioMultiplatform() {
        setupOkioKmpTest()
        val results = GradleRunner.create()
            .withProjectDir(projectDir.root)
            .withArguments(":allTests")
            .run()

        assertTestPassed(results, ":allTests")
    }
}
