package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.*

internal actual fun existsImpl(path: String): Boolean = access(path, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteNoCheckImpl(path: String) {
    if (remove(path) != 0) {
        if (errno == EACCES) {
            if (rmdir(path) == 0) return
        }
        throw IOException("Delete failed for $path: ${strerror(errno)?.toKString()}")
    }
}
