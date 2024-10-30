# Module kotlinx-io-core

The module provides core multiplatform IO primitives and integrates it with platform-specific APIs.

`kotlinx-io core` aims to provide a concise but powerful API along with efficient implementation.

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

Example below shows how to manually serialize an object to [BSON](https://bsonspec.org/spec.html) 
and then back to an object using `kotlinx.io`. Please note that the example aimed to show `kotlinx-io` API in action,
rather than to provide a robust BSON-serialization.
```kotlin
data class Message(val timestamp: Long, val text: String) {
    companion object
}

fun Message.toBson(sink: Sink) {
    val buffer = Buffer()
    with (buffer) {
        writeByte(0x9)                          // UTC-timestamp field
        writeString("timestamp")                // field name
        writeByte(0)
        writeLongLe(timestamp)                  // field value
        writeByte(0x2)                          // string field
        writeString("text")                     // field name
        writeByte(0)
        writeIntLe(text.utf8Size().toInt() + 1) // field value: length followed by the string
        writeString(text)
        writeByte(0)
        writeByte(0)                            // end of BSON document
    }

    // Write document length and then its body
    sink.writeIntLe(buffer.size.toInt() + 4)
    buffer.transferTo(sink)
    sink.flush()
}

fun Message.Companion.fromBson(source: Source): Message {
    source.require(4)                                    // check if the source contains length
    val length = source.readIntLe() - 4L
    source.require(length)                               // check if the source contains the whole message

    fun readFieldName(source: Source): String {
        val delimiterOffset = source.indexOf(0)          // find offset of the 0-byte terminating the name
        check(delimiterOffset >= 0)                      // indexOf return -1 if value not found
        val fieldName = source.readString(delimiterOffset) // read the string until terminator
        source.skip(1)                                   // skip the terminator
        return fieldName
    }

    // for simplicity, let's assume that the order of fields matches serialization order
    var tag = source.readByte().toInt()                // read the field type
    check(tag == 0x9 && readFieldName(source) == "timestamp")
    val timestamp = source.readLongLe()                // read long value
    tag = source.readByte().toInt()
    check(tag == 0x2 && readFieldName(source) == "text")
    val textLen = source.readIntLe() - 1L              // read string length (it includes the terminator)
    val text = source.readString(textLen)                // read value
    source.skip(1)                                     // skip terminator
    source.skip(1)                                     // skip end of the document
    return Message(timestamp, text)
}
```

# Package kotlinx.io

Core IO primitives.

#### Thread-safety guarantees

Until stated otherwise, types and functions provided by the library are not thread safe.
