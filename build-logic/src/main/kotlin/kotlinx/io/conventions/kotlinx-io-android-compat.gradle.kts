/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import ru.vyarus.gradle.plugin.animalsniffer.AnimalSnifferExtension

apply(plugin = "ru.vyarus.animalsniffer")

project.plugins.withType(JavaPlugin::class.java) {
    configure<AnimalSnifferExtension> {
        sourceSets = listOf((project.extensions.getByName("sourceSets") as SourceSetContainer).getByName("main"))
    }
    val signature: Configuration by configurations
    dependencies {
        // Use the same API level as OkHttp
        signature("net.sf.androidscents.signature:android-api-level-21:5.0.1_r2@signature")
    }
}
