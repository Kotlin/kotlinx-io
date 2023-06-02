# Module kotlinx-io-core

The module provides core multiplatform IO primitives and integrates it with platform-specific APIs.
`kotlinx-io` aims to provide a concise but powerful API along with efficient implementation.

The main interfaces for the IO interaction are [kotlinx.io.Source] and [kotlinx.io.Sink] providing buffered read and 
write operations for integer types, byte arrays, and other sources and sinks. There are also extension functions
bringing support for strings and other types. 
Implementations of these interfaces are built on top of [kotlinx.io.Buffer], [kotlinx.io.RawSource],
and [kotlinx.io.RawSink].

A central part of the library, [kotlinx.io.Buffer], is a container optimized to reduce memory allocations and to avoid
data copying when possible.

[kotlinx.io.RawSource] and [kotlinx.io.RawSink] are interfaces aimed for integration with anything that can provide 
or receive data: network interfaces, files, etc. The module provides integration with some platform-specific IO APIs,
but if something not yet supported by the library needs to be integrated, then these interfaces are exactly what should 
be implemented for that.

Here is an example showing how to manually create BSON  `kotlinx.io`
```kotlin

// TODO: can't find out concise and simple exa,ple that will use multiple features of the library.

```

# Package kotlinx.io

Core IO primitives.

# Package kotlinx.io.files

Basic API for working with files.