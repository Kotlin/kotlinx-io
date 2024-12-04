# Module kotlinx-io-okio

The module bridges `Okio` `3.x` interfaces with `kotlinx-io` and vice versa.

[okio.Source] could be wrapped into [kotlinx.io.RawSource]
using [kotlinx.io.okio.asKotlinxIoRawSource], and [kotlinx.io.RawSource]
could be wrapped into [okio.Source] using [kotlinx.io.okio.asOkioSource].

Similar pair of functions is also defined for [okio.Sink]
and [kotlinx.io.RawSink]: [kotlinx.io.okio.asKotlinxIoRawSink], [kotlinx.io.okio.asOkioSink].

Wrappers translate IO-exceptions thrown from an underlying library into exceptions provided by a wrapping library.
I.e, if an [okio.Source] wrapped into [kotlinx.io.RawSource]
throws [okio.IOException]
during [kotlinx.io.RawSource.readAtMostTo] (or any other) call, that exception will be caught and a [kotlinx.io.IOException] will be thrown.
The same translation is performed in the opposite direction as well.

`ByteString` classes provided by both libraries could also be converted one into another: [kotlinx.io.okio.toKotlinxIoByteString],
[kotlinx.io.okio.toOkioByteString].

# Package kotlinx.io.okio

`kotlinx-io` <-> `Okio` adapters.

#### Thread-safety guarantees

Until stated otherwise, types and functions provided by the library are not thread safe.
