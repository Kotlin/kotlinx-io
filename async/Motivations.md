## Foreword

The reasoning for implementing async API was described [above](https://github.com/Kotlin/kotlinx-io/issues/163#issue-1787883687).

Here are some thoughts and observations that directed the design of the proposed async API.

## Design direction

There are several approaches to the asynchronous IO API that I would split into two major categories:
- APIs supporting asynchronous (or suspending, which I will interchangeably) fine-grained typed IO operations 
(like [tokio](https://docs.rs/tokio/latest/tokio/#asynchronous-io), [ktor-io](https://api.ktor.io/ktor-io/io.ktor.utils.io/index.html));
- APIs supporting only bulk asynchronous IO (like [.NET IO Pipelines](https://learn.microsoft.com/en-us/dotnet/standard/io/pipelines), [Java asynchronous channels](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/nio/channels/AsynchronousByteChannel.html)).

The first category seems to provide a very convenient API 
by making all familiar synchronous IO operations asynchronous, but there is a price for this convenience.

From a user perspective, such APIs encourage writing a code having many asynchronous calls
to read or write small portions of data.
In Kotlin, each suspendable function has a small overhead compared to regular synchronous functions, and
when there are lots of suspendable calls, it might lead to a worse performance. 
Here is a good discussion of that: https://github.com/square/okio/issues/814#issuecomment-735286404

From a library developer perspective, having an asynchronous API mirroring the synchronous API is a burden,
as each of the synchronous functions probably requires an asynchronous counterpart
and each of them has to be implemented, documented, tested, and supported.

On the contrary, asynchronous APIs providing only bulk operations reduce the scope of asynchronous code
and encourage fetching as much data as needed asynchronously, but process it then synchronously.
From a user perspective, it may lead to a better performance.
From a developer perspective, it reduces the amount of code that needs to be written and maintained as a bulk API
is usually much smaller and simpler.

Yet another observation regarding the data formats being actively used for data exchange is that the size
of the whole message/packet/unit of data transfer is usually known in advance (for protocols having fixed-size
frames), or explicitly encoded in a binary message (like in the [BSON format](https://bsonspec.org/spec.html)),
or the message consists of multiple chunks whose size
is encoded in a header (like in the [PNG image format](http://www.libpng.org/pub/png/spec/1.2/PNG-Structure.html),
[gRPC over HTTP2](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md),
or [Thrift RPC framing transport](https://github.com/apache/thrift/blob/master/doc/specs/thrift-rpc.md#framed-vs-unframed-transport)),
or the message is terminated by a special value (like null-terminated strings in a [GZip header](https://datatracker.ietf.org/doc/html/rfc1952.html#page-5)).
So it's relatively easy to use bulk IO API with such formats: we need to fetch a header containing the payload size and
then read the whole payload (that is obviously not the case for large messages, but for them, we will still know 
the exact number of bytes to read and can load the data in batches).

Described above was the main reasoning behind the proposed `kotlinx-io` Async API.

## Asynchronous kotlinx-io API

As a foundation for the asynchronous IO, two interfaces mirroring their blocking counterparts in terms of naming 
and functionality are proposed:
```kotlin
public interface AsyncRawSink {
    public suspend fun write(buffer: Buffer, bytesCount: Long)
    public suspend fun flush()
    public suspend fun close()
}

public interface AsyncRawSource : AutoCloseable {
    public suspend fun readAtMostTo(buffer: Buffer, bytesCount: Long): Long
    override fun close()
}
```
These interfaces aim to implement asynchronous sources and sinks with the same semantics as the synchronous 
RawSink and RawSource.

But for the buffered sinks and sources, there is no one-to-one mapping between the synchronous and asynchronous API:
```kotlin
public class AsyncSource(private val source: AsyncRawSource) : AsyncRawSource {
    public val buffer: Buffer

    public suspend fun await(until: AwaitPredicate): Unit
    public suspend fun tryAwait(until: AwaitPredicate): Boolean

    override suspend fun readAtMostTo(buffer: Buffer, bytesCount: Long): Long
    override fun close()
}

public class AsyncSink(private val sink: AsyncRawSink) : AsyncRawSink {
    public val buffer: Buffer = Buffer()
    
    override suspend fun write(buffer: Buffer, bytesCount: Long)
    override suspend fun flush()
    override suspend fun close()
}
```

These classes don't provide the same future-rich interface as Sink or Source.
Instead, they only encapsulate a buffer and provide functions to asynchronously fill it with data
until a condition expressed using `AwaitPredicate` is met, or flush the data to the underlying sink.
It assumed that the existing Buffer's API as well as all the existing extensions could be used for parsing the
data once it is fetched as well as use it to serialize a message that then will be sent to a sink. 

`AwaitPredicate` interface aimed to inspect the already received data and if more data is needed according
to a criterion, fetch it and check again:

```kotlin
public interface AwaitPredicate {
    public suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean
}
```

There will be some predefined predicates checking for the minimum number of bytes available, 
underlying source's exhaustion, and presence of particular values in the fetched data.

In the simplest form, a predicate might look like this:
```kotlin
public class MinNumberOfBytesAvailable : AwaitPredicate {
    override suspend fun apply(buffer: Buffer, fetchMore: suspend () -> Boolean): Boolean {
        while (buffer.size < bytesCount && fetchMore()) { /* do nothing */ }
        return buffer.size >= bytesCount
    } 
}
```

The `buffer` is exactly the same buffer encapsulated by the `AsyncSource`.
If the buffer already contains data fulfilling a predicate when `await` or `tryAwait` is called, then
it is assumed that no more data will be fetched from the underlying source (but it's, of course, up a predicate's implementation).

Here's how the [BSON reading/writing example](https://fzhinkin.github.io/kotlinx-io-dokka-docs-preview/kotlinx-io-core/index.html) from the `kotlinx-io-core` module will look with the async API:
```kotlin
suspend fun Message.toBson(sink: AsyncSink) {
    val buffer = Buffer()
    with (buffer) {
        writeByte(0x9)                          // UTC-timestamp field
        writeString("timestamp")               // field name
        writeByte(0)
        writeLongLe(timestamp)                  // field value
        writeByte(0x2)                          // string field
        writeString("text")                    // field name
        writeByte(0)
        writeIntLe(text.utf8Size().toInt() + 1) // field value: length followed by the string
        writeString(text)
        writeByte(0)
        writeByte(0)                            // end of BSON document
    }

    // Write document length and then its body
    sink.buffer.writeIntLe(buffer.size.toInt() + 4)
    buffer.transferTo(sink.buffer)
    sink.flush()
}

suspend fun Message.Companion.fromBson(source: AsyncSource): Message {
    source.await(AwaitPredicate.dataAvailable(4))          // check if the source contains length
    val buffer = source.buffer
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

The only suspending calls are bulk reading and flushing, while almost all the parsing and all the marshaling are done synchronously.
