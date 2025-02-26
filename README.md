# kotlinx-io

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/github/license/kotlin/kotlinx-io)](LICENSE)
[![Download](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-io-core)](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-io-core/)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![TeamCity build](https://img.shields.io/teamcity/build/s/KotlinTools_KotlinxIo_BuildAggregated.svg?server=http%3A%2F%2Fteamcity.jetbrains.com)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxIo_BuildAggregated&guest=1)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://kotlin.github.io/kotlinx-io/)

A multiplatform Kotlin library providing basic IO primitives. `kotlinx-io` is based on [Okio](https://github.com/square/okio) but does not preserve backward compatibility with it.

## Overview
**kotlinx-io** is built around `Buffer` - a mutable sequence of bytes.

`Buffer` works like a queue, allowing to read data from its head or to write data to its tail.
`Buffer` provides functions to read and write data of different built-in types, and to copy data to or from other `Buffer`s.
Depending on the target platform, extension functions allowing data exchange with platform-specific types are also available.

A `Buffer` consists of segments organized as a linked list: segments allow reducing memory allocations during the buffer's expansion and copy,
with the latter achieved by delegating or sharing the ownership over the underlying buffer's segments with other buffers.

**kotlinx-io** provides interfaces representing data sources and destinations - `Source` and `Sink`,
and in addition to the *mutable* `Buffer` the library also provides an *immutable* sequence of bytes - `ByteString`.

An experimental filesystem support is shipped under the `kotlinx.io.files` package,
which includes the `FileSystem` interface and its default implementation - `SystemFileSystem`.

`FileSystem` provides basic operations for working with files and directories, which are represented by yet another class under the same package - `Path`.

There are several `kotlinx-io` modules:
- [kotlinx-io-bytestring](./bytestring) - provides `ByteString`.
- [kotlinx-io-core](./core) - provides IO primitives (`Buffer`, `Source`, `Sink`), filesystems support, depends on `kotlinx-io-bytestring`.
- [kotlinx-io-okio](./integration/okio) - bridges `kotlinx-io` and `Okio` `ByteString`, `kotlinx.io.RawSource` and `okio.Source`, `kotlinx.io.RawSink` and `okio.Sink`. 

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

### Gradle

Make sure that you have `mavenCentral()` in the list of repositories:
```kotlin
repositories {
    mavenCentral()
}
```

Add the library to dependencies:
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
}
```

In multiplatform projects, add a dependency to the `commonMain` source set dependencies:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
            }
        }
    }
}
```

### Maven

Add the library to dependencies:
```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-io-core-jvm</artifactId>
    <version>0.7.0</version>
</dependency>
```

### JPMS support

On JVM, `kotlinx-io` supports Java Modules:
- `kotlinx-io-bytestring` library provides `kotlinx.io.bytestring` module; 
- `kotlinx-io-core` library provides `kotlinx.io.core` module.
- `kotlinx-io-okio` library provides `kotlinx.io.okio` module.

Read [this](https://kotlinlang.org/docs/gradle-configure-project.html#configure-with-java-modules-jpms-enabled) article 
for details on how to configure a Gradle project to utilize JPMS.

### Android

`kotlinx-io` is not tested on Android on a regular basis,
but the library is compatible with Android 5.0+ (API level 21+).

## Contributing

Read the [Contributing Guidelines](CONTRIBUTING.md).

## Code of Conduct
This project and the corresponding community are governed by the [JetBrains Open Source and Community Code of Conduct](https://confluence.jetbrains.com/display/ALL/JetBrains+Open+Source+and+Community+Code+of+Conduct). Please make sure you read it.

## License
kotlinx-io is licensed under the [Apache 2.0 License](LICENSE).

## Credits

Thanks to everyone involved in the project.

An honorable mention goes to the developers of [Okio](https://square.github.io/okio/) 
that served as the foundation for `kotlinx-io` and to [Jesse Wilson](https://github.com/swankjesse),
for the help with `Okio` adaption, his suggestions, assistance and guidance with `kotlinx-io` development.
