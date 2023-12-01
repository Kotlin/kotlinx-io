/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.dokka.gradle.*
import java.net.*

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        if (name.endsWith("Main")) {
            sourceLink {
                // sources are located in projectDir/PLATFORM/src
                // where the PLATFORM could be jvm, js, darwin, etc.
                // configuration happens in kotlinx-io-multiplatform.gradle.kts:KotlinSourceSet.configureSourceSet
                val platform = name.dropLast(4)
                val relPath = rootProject.projectDir.toPath().relativize(projectDir.toPath())
                localDirectory.set(projectDir.resolve("$platform/src"))
                remoteUrl.set(URL("https://github.com/kotlin/kotlinx-io/tree/master/$relPath/$platform/src"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
