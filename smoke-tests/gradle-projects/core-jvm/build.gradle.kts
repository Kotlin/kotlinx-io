plugins {
    id("kotlinx-io-smoke-test-jvm")
}

val kotlinxIoVersion: String = project.findProperty("smokeTest.kotlinxIoVersion")?.toString() ?: version.toString()

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:$kotlinxIoVersion")
}