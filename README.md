# kotlinx-io

[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-io.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22kotlinx-io%22)
[![Kotlin](https://img.shields.io/badge/kotlin-1.8.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![TeamCity build](https://img.shields.io/teamcity/build/s/KotlinTools_KotlinxIo_Build.svg?server=http%3A%2F%2Fteamcity.jetbrains.com)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxIo_Build&guest=1)

A multiplatform Kotlin library providing basic IO primitives. `kotlinx-io` is based on [Okio](https://github.com/square/okio) but does not preserve backward compatibility with it.

## Overview
The library is built around `Buffer` - a mutable sequence of bytes. `Buffer` works like a queue allowing one to read data from its head or write data to its tail.

`Buffer` provides functions to read and write data of different built-in types and copy data to or from other buffers. Depending on a target platform, extension functions allowing data exchange with platform-specific types are also provided.

`Buffer` consists of segments organized as a linked list. Segments allow reducing memory allocations during the buffer's expansion and copying. The latter is achieved by delegating or sharing the ownership over the underlying buffer's segments with other buffers.

The library also provides interfaces representing data sources and destinations - `Source` and `Sink`.

## Using in your projects

> Note that the library is experimental, and the API is subject to change.

### Gradle

Make sure that you have `mavenCentral()` in the list of repositories:
```
repositories {
    mavenCentral()
}
```

Add the library to dependencies:
```
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-io:0.2.0-alpha-dev1")
}
```

### Maven

Add the library to dependencies:
```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-io</artifactId>
    <version>0.2.0-alpha-dev1</version>
</dependency>
```

## Contributing

Read the [Contributing Guidelines](https://github.com/Kotlin/kotlinx-io/blob/master/CONTRIBUTING.md).

## Reporting issues/Support

Please use [GitHub issues](https://github.com/Kotlin/kotlinx-io/issues) for filing feature requests and bug reports.

## Code of Conduct
This project and the corresponding community are governed by the [JetBrains Open Source and Community Code of Conduct](https://confluence.jetbrains.com/display/ALL/JetBrains+Open+Source+and+Community+Code+of+Conduct). Please make sure you read it.

## License
kotlinx-io is licensed under the [Apache 2.0 License](LICENSE).