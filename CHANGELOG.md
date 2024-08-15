# CHANGELOG

## 0.5.2
> Published 15 August 2024
 
### Features
- Unsafe API method signatures were updated to return number of written/read bytes [#360](https://github.com/Kotlin/kotlinx-io/issues/360)

  Note that it's a breaking change as return type is a part of method signature.
  However, updated methods are all inline, so in general, 
  projects depending on libraries that were compiled against the previous version
  should not experience binary incompatibility issues after adding a dependency on a never `kotlinx-io` version.
  
- Added call-in-place contracts to Unsafe API methods [#361](https://github.com/Kotlin/kotlinx-io/issues/361) 
- Updated Gradle and some dependencies [#353](https://github.com/Kotlin/kotlinx-io/pull/353)
- Improved build scripts [#371](https://github.com/Kotlin/kotlinx-io/pull/371)
- Removed the `Buffer.buffer` backing field [#366](https://github.com/Kotlin/kotlinx-io/pull/366)

## 0.5.1
> Published 15 July 2024

### Bugfixes
- Fixed a bug in segment pool implementation affecting a second level pool uses.

## 0.5.0
> Published 12 July 2024

### Features
- Provided an API allowing direct access to Buffer and Segment internals [#135](https://github.com/Kotlin/kotlinx-io/issues/135), [#166](https://github.com/Kotlin/kotlinx-io/issues/166)

  The API is unsafe, delisted from public docs and requires explicit opt-in. It's recommended to
  avoid this API unless you're working on integration with other APIs (like, `java.nio` or
  `io_uring`, for example).

- Improved the way segment pooling is working on JVM [#352](https://github.com/Kotlin/kotlinx-io/pull/352)

  Now sharing a segment won't make an original segment and all its copies recyclable.
  Instead, the last remaining copy will be placed back into the pool when recycled.
  Segments are no longer allocated or lost when taking or recycling a segment from pool
  under a high contention due to concurrent requests.
  Size of the segment pool on the JVM could now be statically configured by setting a system property
  `kotlinx.io.pool.size.bytes`.

## 0.4.0
> Published 6 June 2024

### Features
- Updated Kotlin to 2.0 [#327](https://github.com/Kotlin/kotlinx-io/pull/327)

## 0.3.5
> Published 17 May 2024

### Features
- Provided an extension function to write `CharSequence`s (`Sink.writeString`) [#318](https://github.com/Kotlin/kotlinx-io/pull/318)
- Various minor improvements in build scripts and docs

## 0.3.4
> Published 8 May 2024

### Features
- Kotlin updated to 1.9.24 [#315](https://github.com/Kotlin/kotlinx-io/pull/315)
- Supported `linuxArm32Hfp` target [#303](https://github.com/Kotlin/kotlinx-io/issues/303)
- Enabled KLib ABI validation
- Provided extension functions to read (`Source.readCodePointValue`) and write (`Sink.writeCodePointValue`)
  Unicode code point values [#307](https://github.com/Kotlin/kotlinx-io/issues/307)
- Provided function to list directories (`FileSystem.list`) [#222](https://github.com/Kotlin/kotlinx-io/issues/222)

## 0.3.3
> Published 22 Apr 2024

### Features
- Reimplemented file metadata gathering without required reasoning APIs on Apple targets [#297](https://github.com/Kotlin/kotlinx-io/pull/297)

### Bugfixes
- For JS target, Webpack should no longer report missing nodejs modules during compilation [#285](https://github.com/Kotlin/kotlinx-io/pull/285)

## 0.3.2
> Published 18 Mar 2024

### Features
- Implemented basic filesystem support for Wasm WASI target [#257](https://github.com/Kotlin/kotlinx-io/pull/257)
- Enabled native benchmarks by default [#263](https://github.com/Kotlin/kotlinx-io/pull/263)

## 0.3.1
> Published 2 Feb 2024

### Features
- Updated Kotlin to `1.9.22`
- Enabled Wasm Wasi target [#236](https://github.com/Kotlin/kotlinx-io/pull/236) (Note that neither of Wasm targets has filesystem support yet)
- Support path resolution [#228](https://github.com/Kotlin/kotlinx-io/pull/228)

### Bugfixes
- Fixed `Path::parent` behavior on Windows [#227](https://github.com/Kotlin/kotlinx-io/pull/227)
- Aligned behavior of `FileSystem::sink` and `FileSystem::source` across all platforms [#252](https://github.com/Kotlin/kotlinx-io/pull/252)
- Fixed handling of paths containing a whitespace character [#248](https://github.com/Kotlin/kotlinx-io/pull/248)

## 0.3.0
> Published 13 Sep 2023

### Features
- Enabled Wasm target [#164](https://github.com/Kotlin/kotlinx-io/issues/164)
- Added Sink/Source integration with Apple's NSInputStream and NSOutputStream ([#174](https://github.com/Kotlin/kotlinx-io/pull/174))
- Added extension functions integrating ByteString with Base64 and HexFormat APIs ([#149](https://github.com/Kotlin/kotlinx-io/issues/149))
- Added extension functions to read and write floating point numbers ([#167](https://github.com/Kotlin/kotlinx-io/issues/167))
- Extended filesystems support by adding functions to create and delete files and directories, check their existence,
  perform atomic move, and get file size ([#211](https://github.com/Kotlin/kotlinx-io/issues/211), 
  [#214](https://github.com/Kotlin/kotlinx-io/issues/214)).
  Also extended Path's API to request Path's parent and to get file's name 
  ([#206](https://github.com/Kotlin/kotlinx-io/issues/206), [#212](https://github.com/Kotlin/kotlinx-io/issues/212)).
- Updated Kotlin version to 1.9.10

### Bugfixes
- Fixed undefined behavior in the ByteString's hashCode computation on native targets ([#190](https://github.com/Kotlin/kotlinx-io/issues/190))
- Fixed compatibility issues with Android API 25 and below ([#202](https://github.com/Kotlin/kotlinx-io/issues/202))

## 0.2.1
> Published 11 Jul 2023
 
The release includes a bug fix solving the issue with dependency management. 

### Bugfixes
- Fixed the dependency type for `bytesting` module,
  it is no longer required to explicitly specify it when using `kotlinx-io-core` 
  ([#169](https://github.com/Kotlin/kotlinx-io/issues/169)).

## 0.2.0
> Published 3 Jul 2023

Initial release of the new `kotlinx-io` version implemented based on `Okio` library.

### Features
- A trimmed-down and reworked version of the core Okio API
  ([#132](https://github.com/Kotlin/kotlinx-io/issues/132), [#137](https://github.com/Kotlin/kotlinx-io/issues/137))
- ByteString implementation ([#133](https://github.com/Kotlin/kotlinx-io/issues/133))

---
Changelog for previous versions may be found in [CHANGELOG-0.1.X.md](CHANGELOG-0.1.X.md)
