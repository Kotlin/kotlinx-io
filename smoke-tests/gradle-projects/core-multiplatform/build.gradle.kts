plugins {
    id("kotlinx-io-smoke-test-multiplatform")
}

val kotlinxIoVersion: String = project.findProperty("smokeTest.kotlinxIoVersion")?.toString() ?: version.toString()

kotlin.sourceSets {
    val commonMain by getting {
        dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:$kotlinxIoVersion")
        }
    }
}