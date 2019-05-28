
`kotlinx-io` is a multiplatform library for 
processing binary data, 
working with memory blocks,
interacting with the platform,
and performing other low level operations.  

![Experimental](https://img.shields.io/badge/kotlinx-experimental-orange.svg?style=flat)

NOTE: This library is *experimental*. Any API is a subject to change.

# Structure

NOTE: This part represents the target vision of the package. Most modules are still unavailable.

`kotlinx-io` package has many modules and you can use as much as you need. 

* `core` – defines all the low-level functionality for working with binary data and basic text.
* `async` – (unavailable) implements asynchronous versions of input and output data streams.
* `platform` – (unavailable) provides low-level platform facilities such as reading or writing from/to a file.
* `sockets` – (unavailable) provides low-level functionality for interacting with network.
* `cryptography` – (unavailable) provides encryption & decryption functionality.
* `compression` – (unavailable) provides compression & decompression functionality.
* `files` – (unavailable) provides advanced file system functionality such as working with paths and directories. 

# Core 

This module provides few core I/O primitives that are used across other modules and can be used to define
custom binary inputs and outputs, as well as processing raw memory.

* `Buffer` – represents a continuous memory block of specific size. 
Provides direct positional read and write operations for primitives and arrays of primitives. 
* `Input` – represents a source of bytes. Provides sequential reading functionality and a special `preview` mode
for processing bytes without discarding them. 
* `Output` – represents a destination for bytes. Provides sequential writing functionality. 
* `Bytes` – represents binary data of arbitrary size, potentially spanned across several buffers.
Can be built using using `Output` and can be read using `Input`.

It also has basic facilities for working with text, with UTF-8 implemented efficiently in core, 
and `Charsets` giving access to platform-dependent functionality for converting text into bytes and back.

## Buffers

Buffer is direct representation of memory on the target platform implemented using efficient platform-dependent 
mechanisms. A buffer of arbitrary size can be allocated and released using `PlatformBufferAllocator`. 
It is user's responsibility to release an allocated buffer. 

```kotlin
    val buffer = PlatformBufferAllocator.allocate(8) // allocates a buffer of 8 bytes
    buffer.storeLongAt(0, 123451234567890L) // stores a long value at offset 0
    val longValue = buffer.loadLongAt(0) // reads back a long value
```

All operations with a `Buffer` are performed in network byte order (Big-Endian). 
There are helper functions `reverseByteOrder` defined for all primitive types to reverse the byte order when it is needed. 

## Inputs

An `Input` is a high-performance buffered entity for reading data from an underlying source.  
It is an abstract class with only few abstract methods and a plentiful of convenience built around them. 
There are functions to read primitives, arrays of primitives, higher-level extension methods for reading UTF-8 text,
text encoded with a custom `Charset`, and more. One can define any other read methods using extensions and provided primitives.

`Input` design doesn't provide facilities for direct manipulation of the current reading position, 
but instead it has the `preview` mechanism which we believe is a lot safer, efficient and enough for most look-ahead
scenarios.

Preview operation instructs `Input` to start accumulating buffers instead of discarding them when they are exhausted,
thus making it possible to revert to the initial position without performing additional I/O operations. 

```kotlin
    input.readLong() // (0) reads long value and discards bytes
    input.preview {  // (1) begins preview operation and stops discarding bytes
        readShort() // (2) read short value and keep the bytes
    } // completes preview operation and rewinds the input to the state (1) 
    input.readShort()  // (3) reads short value from (2) again
```   

Note that `preview` function provides another, nested `Input` to the lambda as a receiver 
which should be used for all preview reads. 
Implementation can choose to alter original `Input` state or create a new instance, 
so one should always be using the instance provided as a receiver to `preview`. 

Preview operations can be nested, each keeping its own state and position, thus making it possible to compose
operations on Inputs.

## Outputs

An `Output` is a high-performance buffered entity for writing data to an underlying destination. 
Like `Input`, it provides all the primitive operations as well as a number of convenience functions for text output.

Similarly, `Output` doesn't provide a mechanism to rewind backwards and update data, but using `Bytes` one can easily
implement complex scenarios such as writing a size before a block, calculating hashes and so on. 

## Bytes

A `Bytes` type is useful for transferring data between various endpoints, accumulating data in memory or sending 
repetitive bytes to different outputs. 

`Bytes` can be produced by building function `buildBytes { … }` where lambda has an `Output` as a receiver, 
thus making it possible to conveniently generate content, or use in any I/O operations or custom user's functions. 

```kotlin
    val bytes = buildBytes {
        writeLong(0x0001020304050607)
        writeShort(0x0809)
    }
```

When you have a `Bytes` instance, you can know the number of bytes stored, and can obtain an `Input` to read these bytes.
Creating an `Input` is a zero-copy operation, underlying mechanics simply reuses buffers for reading data.

```kotlin
    val input = bytes.input()
    input.readLong() 
```  

Writing such an instance into `Output` is also zero-copy operation, since implementation will send existing buffers
to the underlying destination. 

```kotlin
    output.writeBytes(bytes)
```

Combining these features makes it possible to write domain-specific functions for complex data writing:

```kotlin
fun Output.writeWithSizeAndHash(writer: Output.()->Unit) {
    val bytes = buildBytes(writer)
    writeInt(bytes.size)
    writeBytes(bytes)
    val hash = bytes.input().calculateHash()
    writeLong(hash)
}
``` 

## Text

[TBD] Efficient UTF-8 and platform-dependent Charsets

## Pools

[TBD] Allocating and releasing a buffer each time one is needed can be inefficient, 
so the package provides facilities for buffer pools.

# Async

[TBD] `InputChannel` and `OutputChannel` as an asynchronous (suspending) versions of `Input` and `Output`
 

# Platform

[TBD]
* `FileInput` and `FileOutput` with a very limited set of operations such as `open`. No paths, no directories, no access control.
* `Process` type for launching an external processes, and interacting with their inputs and outputs.
* `Environment` type for interacting with environment variables.

# Adding a dependency

```gradle
dependencies {
    compile "org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinx_io_version"
}
```

