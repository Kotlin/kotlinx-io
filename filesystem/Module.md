# Module kotlinx-io-filesystem

The module provides experimental files and filesystem support. The API is unstable and will change in the future.

# Package kotlinx.io.files

Basic API for working with files.

#### Thread-safety guarantees

Until stated otherwise, types and functions provided by the library are not thread safe.

#### Known issues

[//]: <> (TODO: Link to SystemFileSystem doesn't work)
- For JS and Wasm, [kotlinx.io.files.SystemFileSystem] is supported only in the NodeJs environment. Attempts to use it
in the browser environment will result in a runtime error.
- [#312](https://github.com/Kotlin/kotlinx-io/issues/312) For `wasmWasi` target, directory listing ([kotlinx.io.files.FileSystem.list]) does not work with NodeJS runtime on Windows,
as `fd_readdir` function is [not implemented there](https://github.com/nodejs/node/blob/6f4d6011ea1b448cf21f5d363c44e4a4c56ca34c/deps/uvwasi/src/uvwasi.c#L19).
