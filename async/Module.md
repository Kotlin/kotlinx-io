# Module kotlinx-io-async

The module provides multiplatform asynchronous IO primitives.

`kotlinx-io-async` inherited naming conventions from the `kotlinx-io-core` and provides [kotlinx.io.async.AsyncRawSink] 
and [kotlinx.io.async.AsyncRawSource] interfaces as base for asynchronous sinks and sources, 
and buffered [kotlinx.io.async.AsyncSink] and [kotlinx.io.async.AsyncSource] aimed to be used in the client code.

[kotlinx.io.async.AsyncRawSink] and [kotlinx.io.async.AsyncRawSource] interfaces mirror their blocking counterparts,
but allow suspension during read, write, flush and close (in case of a sink) operations. Implementing these interfaces
is much like implementing interfaces for blocking IO, but implementors should consider supporting
[cooperative cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html#cancellation-is-cooperative) 
when possible.

Unlike buffered sink and source from the `kotlinx-io-core`, [kotlinx.io.async.AsyncSink] 
and [kotlinx.io.async.AsyncSource] don't provide methods for reading or writing typed data. Instead, these classes 
provide only a buffer that could be accessed for reading and writing without suspension, and suspendable methods
for filling the buffer with data from an underlying source or flushing the data from the buffer into an underlying sink.
Such design is aimed to reduce overhead of multiple short IO operations
by encouraging users to use suspendable functions mainly for bulk IO.

[kotlinx.io.async.AsyncSource] allows fetching the data 
until a condition expressed by the particular [kotlinx.io.async.AwaitPredicate] is met.
[kotlinx.io.async.AwaitPredicate] has a single method accepting a buffer with already received data that
need to be checked in order to determine whether the condition is met and a callback to fetch more data
into that buffer.
There are few [kotlinx.io.async.AwaitPredicate] implementations that should fulfill basic needs: checking if enough
bytes were received, if a source was exhausted or if received data contains particular values (like a like separator).

Here's how the example from `kotlinx-io-core` module may look like if we want to read the `Message` asynchronously:
```kotlin
suspend fun Message.Companion.fromBson(source: AsyncSource): Message {
    val buffer = source.buffer                             // the source will fetch data into that buffer
    source.await(AwaitPredicate.dataAvailable(4))          // check if the source contains length
    val length = buffer.readIntLe() - 4L
    source.await(AwaitPredicate.dataAvailable(length))     // check if the source contains the whole message

    fun readFieldName(source: Buffer): String {
        val delimiterOffset = source.indexOf(0)            // find offset of the 0-byte terminating the name
        check(delimiterOffset >= 0)                        // indexOf return -1 if value not found
        val fieldName = source.readString(delimiterOffset) // read the string until terminator
        source.skip(1)                                     // skip the terminator
        return fieldName
    }

    // for simplicity, let's assume that the order of fields matches serialization order
    var tag = buffer.readByte().toInt()                     // read the field type
    check(tag == 0x9 && readFieldName(buffer) == "timestamp")
    val timestamp = buffer.readLongLe()                     // read long value
    tag = buffer.readByte().toInt()
    check(tag == 0x2 && readFieldName(buffer) == "text")
    val textLen = buffer.readIntLe() - 1L                   // read string length (it includes the terminator)
    val text = buffer.readString(textLen)                   // read value
    buffer.skip(1)                                          // skip terminator
    buffer.skip(1)                                          // skip end of the document
    return Message(timestamp, text)
}
```
