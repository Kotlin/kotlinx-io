/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*

fun KotlinMultiplatformExtension.configureNativePlatforms() {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosX64()
    watchosSimulatorArm64()
    // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()
}

private val appleTargets = listOf(
    "iosArm64",
    "iosX64",
    "iosSimulatorArm64",
    "macosX64",
    "macosArm64",
    "tvosArm64",
    "tvosX64",
    "tvosSimulatorArm64",
    "watchosArm32",
    "watchosArm64",
    "watchosX86",
    "watchosX64",
    "watchosSimulatorArm64"
)

private val mingwTargets = listOf(
    "mingwX64"
)

private val linuxTargets = listOf(
    "linuxX64"
)

val nativeTargets = appleTargets + linuxTargets + mingwTargets

/**
 * Creates a source set for a directory that isn't already a built-in platform. Use this to create
 * custom shared directories like `nonJvmMain` or `unixMain`.
 */
fun NamedDomainObjectContainer<KotlinSourceSet>.createSourceSet(
    name: String,
    parent: KotlinSourceSet? = null,
    children: List<String> = listOf()
): KotlinSourceSet {
    val result = create(name)

    if (parent != null) {
        result.dependsOn(parent)
    }

    val suffix = when {
        name.endsWith("Main") -> "Main"
        name.endsWith("Test") -> "Test"
        else -> error("unexpected source set name: ${name}")
    }

    for (childTarget in children) {
        val childSourceSet = get("${childTarget}$suffix")
        childSourceSet.dependsOn(result)
    }

    return result
}

