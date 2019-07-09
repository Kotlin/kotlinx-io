# 0.1.11

* Fixed memory leak when using a byte channel on JVM in a long-running coroutine.

# 0.1.7
> Published 6 Mar 2019

- Introduced ISO-8859-1 on JS and common `Charsets.ISO_8859_1`.
- Deprecated `Input.byteOrder`, `Output.byteOrder`, 
`ByteReadPacket.byteOrder`, `BytePacketBuilder.byteOrder`. 
Introduced read/writeLittleEndian functions instead.
- `CharsetEncoder` and `CharsetDecoder` are made experimental to 
make possible to improve that API in the future.
- Several minor deprecations.
- Renamed `errno` to `errorCode` in `PosixException` to avoid objc export issues.

# 0.1.6
> Published 20 Feb 2019

- Fixed object pool instances disposal on JS and native
- Updated atomicfu to 0.12.2

# 0.1.5
> Published 12 Feb 2019

- `AbstractOutput` is implementable, simplified API
- Fixed ISO-8859-1 and other character encodings on native
- Fixed segfault caused by a wrong character encoding specified
- Introduced initial unsigned types support (#28)
- Introduced initial POSIX synchronous support (#34):
    - added `Input(fileDescriptor)` and `Output(fileDescriptor)`
    - added `read`, `write`, `fread`, `fwrite`, `send`, `receive`, `sendto`, `recvfrom` 
    with `IoBuffer` parameter
- Introduced initial `PosixException` support
- Strengthened internal API restrictions
- Introduced `Input.copyTo(Output)` utility function
- Introduced multiplatform `IOException`
- Introduced `reverseByteOrder` for primitive numeric types
- Fixed several memory management bugs
- Eliminated accidentally used JDK8+ API (#35)
- Fixed loosing trailing bytes in byte channel on native and JS (
    [ktor/787](https://github.com/ktorio/ktor/issues/787), 
    [ktor/920](https://github.com/ktorio/ktor/issues/920) 
    )
- Improved `readDirect`/`writeDirect` functions on platforms.
- Fixed non-local returns from `use {}` block.
- Kotlin 1.3.21

# 0.1.4
> Published 23 Jan 2019

- Fixed byteOrder switch for packets (#30)
- Upgrade to Gradle 4.10 with new metadata
- Kotlin 1.3.20

# 0.1.3
> Published 25 Dec 2018

- Fixed wrong pom dependencies

# 0.1.2
> Published 24 Dec 2018

- Fixed byte channel constructor from an array
- Fixed endGap related errors (#23)
- Introduced suspending consumeEachRemaining (#22)
- Kotlin 1.3.11, kotlinx.coroutines 1.1.0
- Fixed await returned wrong result in sequential implementation (#24)
- `await` and `awaitAtLeast` contract clarified (#24)
- Fixed blocking I/O adapter to use coroutine's event loop

# 0.1.1
> Published 4 Dec 2018

- Fixed ability to implement DefaultPool in common
- Fixed error "Unable to stop reading in state Writing"
- Fixed tryPeek implementation to not consume byte
- Introduced peekCharUtf8
- Added a cpointer constructor to native IoBuffer so that IoBuffer can be used to read AND write on a memory chunk 
- Made ByteChannel pass original cause from the owner job
- Fixed reading UTF-8 lines
- Fixed empty chunk view creation
- Utility functions takeWhile* improvements

# 0.1.0
> Published 15 Nov 2018
Initial release, maven central
