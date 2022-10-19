buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

repositories {
    mavenCentral()
}

allprojects {
    repositories {
        mavenCentral()
    }
}
