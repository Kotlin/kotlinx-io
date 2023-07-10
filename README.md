# kotlinx-io

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-io-core?versionSuffix=0.2.0)](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-io-core/0.2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![TeamCity build](https://img.shields.io/teamcity/build/s/KotlinTools_KotlinxIo_BuildAggregated.svg?server=http%3A%2F%2Fteamcity.jetbrains.com)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxIo_BuildAggregated&guest=1)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://fzhinkin.github.io/kotlinx-io-dokka-docs-preview/)

A multiplatform Kotlin library providing basic IO primitives. `kotlinx-io` is based on [Okio](https://github.com/square/okio) but does not preserve backward compatibility with it.

## Overview
The library is built around `Buffer` - a mutable sequence of bytes. `Buffer` works like a queue allowing one to read data from its head or write data to its tail.

`Buffer` provides functions to read and write data of different built-in types and copy data to or from other buffers. Depending on a target platform, extension functions allowing data exchange with platform-specific types are also provided.

`Buffer` consists of segments organized as a linked list. Segments allow reducing memory allocations during the buffer's expansion and copying. The latter is achieved by delegating or sharing the ownership over the underlying buffer's segments with other buffers.

The library also provides interfaces representing data sources and destinations - `Source` and `Sink`.

In addition to `Buffer`, the library provides an immutable sequence of bytes - `ByteString`.

There are two `kotlinx-io` modules:
- [kotlinx-io-bytestring](./bytestring) - provides `ByteString`.
- [kotlinx-io-core](./core) - provides IO primitives (`Buffer`, `Source`, `Sink`), depends on `kotlinx-io-bytestring`.

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

### Gradle

Make sure that you have `mavenCentral()` in the list of repositories:
```kotlin
repositories {
    mavenCentral()
}
```

Add libraries to dependencies:
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.2.0")
}
```

In multiplatform projects, add a dependency to the `commonMain` source set dependencies:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.2.0")
            }
        }
    }
}
```

### Maven

Add libraries to dependencies:
```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-io-core-jvm</artifactId>
    <version>0.2.0</version>
</dependency>
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-io-bytestring-jvm</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Contributing

Read the [Contributing Guidelines](CONTRIBUTING.md).

## Code of Conduct
This project and the corresponding community are governed by the [JetBrains Open Source and Community Code of Conduct](https://confluence.jetbrains.com/display/ALL/JetBrains+Open+Source+and+Community+Code+of+Conduct). Please make sure you read it.

## License
kotlinx-io is licensed under the [Apache 2.0 License](LICENSE).

## Credits

Thanks to everyone involved in the project.

A honorable mention goes to the developers of [Okio](https://square.github.io/okio/) 
that served as the foundation for `kotlinx-io` and to [Jesse Wilson](https://github.com/swankjesse),
for the help with `Okio` adaption, his suggestions, assistance and guidance with `kotlinx-io` development.
