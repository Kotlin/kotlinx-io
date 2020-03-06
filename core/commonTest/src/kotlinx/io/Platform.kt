package kotlinx.io

enum class Platform {
    JVM,
    NATIVE,
    JS
}

expect val platform: Platform
