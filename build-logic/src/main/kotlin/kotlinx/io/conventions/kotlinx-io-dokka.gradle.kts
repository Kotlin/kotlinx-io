/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")

        sourceLink {
            localDirectory = rootDir
            remoteUrl = URL("https://github.com/kotlin/kotlinx-io/tree/master")
            remoteLineSuffix = "#L"
        }

        // we don't want to advertise `unsafe` APIs in documentation
        perPackageOption {
            suppress = true
            matchingRegex = ".*unsafe.*"
        }

        // as in kotlinx-io-multiplatform.gradle.kts:configureSourceSet
        val platform = name.dropLast(4)
        samples.from(
            "common/test/samples",
            "$platform/test/samples"
        )
    }
}
