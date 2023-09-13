# CHANGELOG
## 0.3.0
> Published XX Sep 2023

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
