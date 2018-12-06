[![Official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.io/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.io/_latestVersion)
[![TeamCity Build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxIo_BuildLinux.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxIo_BuildLinux&branch_KotlinTools_KotlinxIo=%3Cdefault%3E&tab=buildTypeStatusDiv)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

kotlinx-io is a multiplatform library suitable for I/O primitives building and manipulations

![Experimental](https://img.shields.io/badge/kotlinx-experimental-orange.svg?style=flat)

Please note that this library is experimental. Any API is a subject to change.

Best with [kotlinx.coroutines-io](https://github.com/Kotlin/kotlinx.coroutines)

# Setup

```gradle
repositories {
    jcenter()
}

dependencies {
    compile "org.jetbrains.kotlinx:kotlinx-io-jvm:$kotlinx_io_version"
}
```

Use `kotlinx-io-js` with Kotlin JavaScript and `kotlin-io` for common code if you are doing multiplatform module.

# Basic concepts

## IoBuffer

A buffer view is a view to byte buffer (in JVM it could be direct `ByteBuffer`, on JS it could be `ArrayBuffer`). Comparing to java's NIO `ByteBuffer`, `IoBuffer` ...

- should be released via `release()` invocation
- could be copied via `makeView()` that actually doesn't copy any bytes but makes a new view
  - a copy should be released as well
- could be used to read and write, no need to do `flip`
- has a `next` property so it is suitable to make chains of buffer views with no need to allocate any lists or extra arrays
- designed to work with buffer view pool

Note that `IoBuffer` is not concurrent safe however it is safe to do `copy()` and `release()` concurrently (when this makes sense).


## ByteReadPacket

`ByteReadPacket` is a packet consist of a managed sequence of buffer views. So one can easily read from a packet and buffer view segments will be released on the way of reading. As far it contains buffer views, a `ByteReadPacket` instance should be released as well via `release()` invocation at the end however there is no need to do it (but it is allowed to do) if all the bytes were consumed and the packet is empty.

- byte packet is read only
- there is no way to reset/pushback already readen bytes
- every buffer view will be released once it becomes empty
- does support `copy()`, similar to `IoBuffer.copy()` it doesn't copy bytes
- not reusable - once all bytes were read there is no way to reset it to read bytes again - make a copy instead
- supports start gap hint (see byte packet builder)
- provides `java.io.Reader` (reads characters as UTF-8) and `java.io.InputStream` compatibility

```kotlin
suspend fun parse(packet: ByteReadPacket, tee: SendChannel<ByteReadPacket>) {
    tee.send(packet.copy())

    while (!packet.isEmpty()) {
        val size = packet.readInt()
        for (i in 1 .. size) {
            println("number: ${packet.readInt()}")
        }
    }
}
```


## BytePacketBuilder

A packet builder that consists of a sequence of buffer views. It borrows buffer views from a pool on demand and does nevery copy bytes on growth (as it does `ByteArrayOutputStream`). 

- write-only
- has explicit `release()` function to discard all bytes
- `build()` makes an instance of `ByteReadPacket` and resets builder's state to the initial one so builder becomes empty and ready to build another one packet
- supports optimized write byte packet operation: could merge multiple buffers into one if possible (only if bytes quantity is not too large), considers start gap hint as well
- provides `java.io.OutputStream` and `java.lang.Appendable` (appends characters as UTF-8)
- as was noted before it is reusable: another byte packet could be built once `build()` has been invoked to build a previous one or `reset()` to discard all previously written bytes

```kotlin
val packet = buildPacket {
    writeInt(size * 4)
    for (i in 1..size) {
        writeInt(i)
    }
}
```

```kotlin
private fun BytePacketBuilder.writeData() = TODO("write something")

suspend fun loop(destination: SendChannel<ByteReadPacket>) {
    val builder = BytePacketBuilder()

    try {
        while (true) {
            builder.writeData()
            destination.send(builder.build())
        }
    } finally {
        builder.release()
    }
}

```

## ObjectPool

`ObjectPool` is a general purpose lock-free concurrent-safe object pool. It is leak-safe: all object that hasn't been recycled but collected by GC do not cause any issues with a pool but only allocation penalty. Note that it doesn't mean that leaking object will not cause any issues at all as lost objects could hold some native or external resources. The only guarantee is that `ObjectPool` is not going to break if there are lost objects.

```kotlin
val ExampleIntArrayPool = object : DefaultPool<IntArray>(ARRAY_POOL_SIZE) {
    override fun produceInstance(): IntArray = IntArray(ARRAY_SIZE)
}
```

```kotlin
class ExampleDirectByteBufferPool(val bufferSize: Int, size: Int) : DefaultPool<ByteBuffer>(size) {
    override fun produceInstance(): ByteBuffer = java.nio.ByteBuffer.allocateDirect(bufferSize)

    override fun clearInstance(instance: ByteBuffer): ByteBuffer {
        instance.clear()
        return instance
    }

    override fun validateInstance(instance: ByteBuffer) {
        require(instance.isDirect)
        require(instance.capacity() == bufferSize)
    }
}
```




