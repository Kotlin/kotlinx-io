package kotlinx.io.tests

enum class Platform {
    JVM,
    NATIVE,
    JS
}

expect val platform: Platform

val isNative: Boolean get() = platform == Platform.NATIVE