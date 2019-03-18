@file:Suppress("unused", "PackageDirectoryMismatch")

package kotlinx.coroutines.experimental.io

import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.io.ByteChannel
import kotlinx.coroutines.io.ByteReadChannel
import kotlinx.coroutines.io.ReaderJob
import kotlinx.coroutines.io.ReaderScope
import kotlinx.coroutines.io.WriterJob
import kotlinx.coroutines.io.WriterScope
import kotlinx.io.charsets.*
import kotlin.coroutines.*

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias ByteReadChannel = kotlinx.coroutines.io.ByteReadChannel

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias ByteWriteChannel = kotlinx.coroutines.io.ByteWriteChannel

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias ByteChannel = kotlinx.coroutines.io.ByteChannel

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias ReaderJob = kotlinx.coroutines.io.ReaderJob

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias ReaderScope = kotlinx.coroutines.io.ReaderScope

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias WriterJob = kotlinx.coroutines.io.WriterJob

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias WriterScope = kotlinx.coroutines.io.WriterScope

@Deprecated("Use the same type from different package", level = DeprecationLevel.ERROR)
typealias CancellationException = CancellationException

@Deprecated(
    "Use the same function from different package",
    ReplaceWith("kotlinx.coroutines.io.ByteChannel(false)", "kotlinx.coroutines.io.ByteChannel"),
    level = DeprecationLevel.ERROR
)
fun ByteChannel(autoFlush: Boolean = false): kotlinx.coroutines.io.ByteChannel =
    kotlinx.coroutines.io.ByteChannel(autoFlush)

@Deprecated(
    "Use the same function from different package",
    ReplaceWith(
        "kotlinx.coroutines.io.ByteReadChannel(content, offset, length)",
        "kotlinx.coroutines.io.ByteReadChannel"
    ),
    level = DeprecationLevel.ERROR
)
fun ByteReadChannel(content: ByteArray, offset: Int = 0, length: Int = content.size): ByteReadChannel =
    kotlinx.coroutines.io.ByteReadChannel(content, offset, length)

@Deprecated(
    "Use the same function from different package",
    ReplaceWith(
        "kotlinx.coroutines.io.ByteReadChannel(text, charset)",
        "kotlinx.coroutines.io.ByteReadChannel"
    ),
    level = DeprecationLevel.ERROR
)
fun ByteReadChannel(text: String, charset: Charset = Charsets.UTF_8): ByteReadChannel =
    kotlinx.coroutines.io.ByteReadChannel(text, charset)

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use scope.reader instead",
    ReplaceWith(
        "kotlinx.coroutines.io.reader(coroutineContext, channel, parent, block)",
        "kotlinx.coroutines.io.reader"
    ),
    level = DeprecationLevel.ERROR
)
fun reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = kotlinx.coroutines.io.reader(coroutineContext, channel, parent, block)

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use scope.reader instead",
    ReplaceWith(
        "kotlinx.coroutines.io.reader(coroutineContext, autoFlush, parent, block)",
        "kotlinx.coroutines.io.reader"
    ),
    level = DeprecationLevel.ERROR
)
fun reader(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = kotlinx.coroutines.io.reader(coroutineContext, autoFlush, parent, block)

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use scope.writer instead",
    ReplaceWith(
        "kotlinx.coroutines.io.writer(coroutineContext, channel, parent, block)",
        "kotlinx.coroutines.io.writer"
    ),
    level = DeprecationLevel.ERROR
)
fun writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob = kotlinx.coroutines.io.writer(coroutineContext, channel, parent, block)

@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use scope.writer instead",
    ReplaceWith(
        "kotlinx.coroutines.io.writer(coroutineContext, autoFlush, parent, block)",
        "kotlinx.coroutines.io.writer"
    ),
    level = DeprecationLevel.ERROR
)
fun writer(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob = kotlinx.coroutines.io.writer(coroutineContext, autoFlush, parent, block)

