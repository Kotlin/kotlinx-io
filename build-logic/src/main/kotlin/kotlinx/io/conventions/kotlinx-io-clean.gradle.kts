/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

tasks {
    // Workaround for https://youtrack.jetbrains.com/issue/KT-58303:
    // the `clean` task can't delete the expanded.lock file on Windows as it's still held by Gradle, failing the build
    named("clean", Delete::class) {
        setDelete(layout.buildDirectory.asFileTree.matching {
            exclude("tmp/.cache/expanded/expanded.lock")
        })
    }
}

