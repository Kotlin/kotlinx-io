/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.wasi

import kotlin.wasm.WasmImport

// https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md#-fd_closefd-fd---result-errno
@WasmImport("wasi_snapshot_preview1", "fd_close")
internal external fun fd_close(fd: Fd): Int

@WasmImport("wasi_snapshot_preview1", "fd_prestat_get")
internal external fun fd_prestat_get(fd: Fd, resultPtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "fd_prestat_dir_name")
internal external fun fd_prestat_dir_name(fd: Fd, pathPtr: Int, pathLen: Int): Int

@WasmImport("wasi_snapshot_preview1", "fd_read")
internal external fun fd_read(fd: Fd, iovecPtr: Int, iovecLen: Int, resultPtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "fd_readdir")
internal external fun fd_readdir(fd: Fd, bufPtr: Int, bufLen: Int, cookie: Long, resultPtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "fd_datasync")
internal external fun fd_datasync(fd: Fd): Int

@WasmImport("wasi_snapshot_preview1", "fd_sync")
internal external fun fd_sync(fd: Fd): Int

@WasmImport("wasi_snapshot_preview1", "fd_write")
internal external fun fd_write(fd: Fd, iovecPtr: Int, iovecLen: Int, resultPtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "path_create_directory")
internal external fun path_create_directory(fd: Fd, pathPtr: Int, pathLen: Int): Int

// TODO: is flags Int?
@WasmImport("wasi_snapshot_preview1", "path_filestat_get")
internal external fun path_filestat_get(fd: Fd, flags: Int, pathPtr: Int, pathLen: Int, resultPtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "path_open")
internal external fun path_open(
    fd: Fd,
    dirflags: Int, // is it?
    pathPtr: Int, pathLen: Int,
    oflags: Int, // is it?
    fsRightsBase: Long,
    fsRightsInheriting: Long,
    fdFlags: Short, // is it?
    resultPtr: Int
): Int

@WasmImport("wasi_snapshot_preview1", "path_remove_directory")
internal external fun path_remove_directory(fd: Fd, pathPtr: Int, pathLen: Int): Int

@WasmImport("wasi_snapshot_preview1", "path_rename")
external internal fun path_rename(
    oldFd: Fd, oldPathPtr: Int, oldPathLen: Int,
    newFd: Fd, newPathPtr: Int, newPathLen: Int
): Int

@WasmImport("wasi_snapshot_preview1", "path_unlink_file")
external internal fun path_unlink_file(
    fd: Fd, pathPtr: Int, pathLen: Int
): Int

@WasmImport("wasi_snapshot_preview1", "path_readlink")
external internal fun path_readlink(
    fd: Fd, pathPtr: Int, pathLen: Int, bufPtr: Int, bufLen: Int, resultPtr: Int
): Int

@WasmImport("wasi_snapshot_preview1", "path_symlink")
external internal fun path_symlink(oldPathPtr: Int, oldPathLen: Int, fd: Fd, newPathPtr: Int, newPathLen: Int): Int
