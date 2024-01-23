/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.wasi

// https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md
internal enum class Errno(val description: String) {
    success("No error occurred. System call completed successfully"),
    toobig("Argument list too long"),
    acces("Permission denied"),
    addrinuse("Address in use"),
    addrnotavail("Address not available"),
    afnosupport("Address family not supported"),
    again("Resource unavailable, or operation would block"),
    already("Connection already in progress"),
    badf("Bad file descriptor"),
    badmsg("Bad message"),
    busy("Device or resource busy"),
    canceled("Operation canceled"),
    child("No child processes"),
    connaborted("Connection aborted"),
    connrefused("Connection refused"),
    connreset("Connection reset"),
    deadlk("Resource deadlock would occur"),
    destaddrreq("Destination address required"),
    dom("Mathematics argument out of domain of function"),
    dquot("Reserved"),
    exist("File exists"),
    fault("Bad address"),
    fbig("File too large"),
    hostunreach("Host is unreachable"),
    idrm("Identifier removed"),
    ilseq("Illegal byte sequence"),
    inprogress("Operation in progress"),
    intr("Interrupted function"),
    inval("Invalid argument"),
    io("I/O error"),
    isconn("Socket is connected"),
    isdir("Is a directory"),
    loop("Too many levels of symbolic links"),
    mfile("File descriptor value too large"),
    mlink("Too many links"),
    msgsize("Message too large"),
    multihop("Reserved"),
    nametoolong("Filename too long"),
    netdown("Network is down"),
    netreset("Connection aborted by network"),
    netunreach("Network unreachable"),
    nfile("Too many files open in system"),
    nobufs("No buffer space available"),
    nodev("No such device"),
    noent("No such file or directory"),
    noexec("Executable file format error"),
    nolck("No locks available"),
    nolink("Reserved"),
    nomem("Not enough space"),
    nomsg("No message of the desired type"),
    noprotoopt("Protocol not available"),
    nospc("No space left on device"),
    nosys("Function not supported"),
    notconn("The socket is not connected"),
    notdir("Not a directory or a symbolic link to a directory"),
    notempty("Directory not empty"),
    notrecoverable("State not recoverable"),
    notsock("Not a socket"),
    notsup("Not supported, or operation not supported on socket"),
    notty("Inappropriate I/O control operation"),
    nxio("No such device or address"),
    overflow("Value too large to be stored in data type"),
    ownerdead("Previous owner died"),
    perm("Operation not permitted"),
    pipe("Broken pipe"),
    proto("Protocol error"),
    protonosupport("Protocol not supported"),
    prototype("Protocol wrong type for socket"),
    range("Result too large"),
    rofs("Read-only file system"),
    spipe("Invalid seek"),
    srch("No such process"),
    stale("Reserved"),
    timedout("Connection timed out"),
    txtbsy("Text file busy"),
    xdev("Cross-device link"),
    notcapable("Extension: Capabilities insufficient")
}

internal fun Errno(errno: Int): Errno {
    require(errno in Errno.entries.indices) { "Unknown errno: $errno" }
    return Errno.entries[errno]
}

internal enum class FileType {
    unknown,
    block_device,
    character_device,
    directory,
    regular_file,
    socket_dgram,
    socket_stream,
    symbolic_link
}

internal fun FileType(filetype: Byte): FileType {
    val value = filetype.toInt()
    require(value in FileType.entries.indices) { "Unknown file type: $value" }
    return FileType.entries[value]
}

internal enum class Rights {
    fd_datasync,
    fd_read,
    fd_seek,
    fd_fdstat_set_flags,
    fd_sync,
    fd_tell,
    fd_write,
    fd_advise,
    fd_allocate,
    path_create_directory,
    path_create_file,
    path_link_source,
    path_link_target,
    path_open,
    fd_readdir,
    path_readlink,
    path_rename_source,
    path_rename_target,
    path_filestat_get,
    path_filestat_set_size,
    path_filestat_set_times,
    fd_filestat_get,
    fd_filestat_set_size,
    fd_filestat_set_times,
    path_symlink,
    path_remove_directory,
    path_unlink_file,
    poll_fd_readwrite,
    sock_shutdown,
    sock_accept
}

internal fun Iterable<Rights>.toBitset(): Long {
    var bitset = 0L
    for (right in this) {
        bitset = bitset.or(1L shl right.ordinal)
    }
    return bitset
}

internal enum class FdFlags {
    append,
    dsync,
    nonblock,
    rsync
}

internal fun Iterable<FdFlags>.toBitset(): Short {
    var bitset = 0
    for (flag in this) {
        bitset = bitset.or(1 shl flag.ordinal)
    }
    return bitset.toShort()
}

internal enum class LookupFlags {
    symlink_follow
}

internal fun Iterable<LookupFlags>.toBitset(): Int {
    var bitset = 0
    for (flag in this) {
        bitset = bitset.or(1 shl flag.ordinal)
    }
    return bitset
}

internal enum class OpenFlags {
    creat,
    directory,
    excl,
    trunc,
}

internal fun Iterable<OpenFlags>.toBitset(): Int {
    var bitset = 0
    for (flag in this) {
        bitset = bitset.or(1 shl flag.ordinal)
    }
    return bitset
}

internal typealias Fd = Int
