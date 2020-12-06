plugins {
    kotlin("multiplatform") apply false
}

allprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlinx")
    }
}
