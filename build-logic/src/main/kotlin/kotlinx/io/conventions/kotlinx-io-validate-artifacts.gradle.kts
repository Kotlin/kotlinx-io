/*
 * Copyright 2010-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.base
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Paths
import kotlin.math.log

plugins {
    base
}

abstract class ArtifactsCheckTask: DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artifactsDirectory: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val artifactsFile: RegularFileProperty

    @TaskAction
    fun check() {
        val actualArtifacts = artifactsDirectory.files.flatMap { file ->
            file.resolve("org/jetbrains/kotlinx").list()?.toSet() ?: emptySet()
        }.toSortedSet()
        val expectedArtifacts = artifactsFile.asFile.get().readLines().toSet()

        if (expectedArtifacts == actualArtifacts) {
            logger.lifecycle("All artifacts are published")
        } else {
            val missedArtifacts = expectedArtifacts - actualArtifacts
            val unknownArtifacts = actualArtifacts - expectedArtifacts
            val message = "The published artifacts differ from the expected ones." +
                    (if (missedArtifacts.isNotEmpty()) missedArtifacts.joinToString(prefix = "\n\tMissing artifacts: ") else "") +
                    (if (unknownArtifacts.isNotEmpty()) unknownArtifacts.joinToString(prefix = "\n\tUnknown artifacts: ") else "")

            logger.error(message)
            throw GradleException("The published artifacts differ from the expected ones")
        }
    }
}

if (project.properties.contains("kotlinx.io.validateDeployment")) {
    tasks.create("validateDeployment", ArtifactsCheckTask::class.java) {
        artifactsFile.set(project.layout.projectDirectory.file("gradle/artifacts.txt"))
        artifactsDirectory.from(
            file(project.properties["kotlinx.io.validateDeployment"] as String)
        )
    }
}
