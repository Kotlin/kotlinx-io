# CHANGELOG

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
