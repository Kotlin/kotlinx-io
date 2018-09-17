@file:Suppress("unused")

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

typealias ByteReadChannel = kotlinx.coroutines.io.ByteChannel
typealias ByteWriteChannel = kotlinx.coroutines.io.ByteWriteChannel
typealias ByteChannel = kotlinx.coroutines.io.ByteChannel

typealias ReaderJob = kotlinx.coroutines.io.ReaderJob
typealias ReaderScope = kotlinx.coroutines.io.ReaderScope

typealias WriterJob = kotlinx.coroutines.io.WriterJob
typealias WriterScope = kotlinx.coroutines.io.WriterScope

typealias CancellationException = CancellationException

@Deprecated(
    "Use the same function from different package",
    ReplaceWith("kotlinx.coroutines.io.ByteChannel(false)", "kotlinx.coroutines.io.ByteChannel")
)
fun ByteChannel(autoFlush: Boolean = false): kotlinx.coroutines.io.ByteChannel =
    kotlinx.coroutines.io.ByteChannel(false)

@Deprecated(
    "Use the same function from different package",
    ReplaceWith(
        "kotlinx.coroutines.io.ByteReadChannel(content, offset, length)",
        "kotlinx.coroutines.io.ByteReadChannel"
    )
)
fun ByteReadChannel(content: ByteArray, offset: Int = 0, length: Int = content.size): ByteReadChannel =
    kotlinx.coroutines.io.ByteReadChannel(content, offset, length)

@Deprecated(
    "Use the same function from different package",
    ReplaceWith(
        "kotlinx.coroutines.io.ByteReadChannel(text, charset)",
        "kotlinx.coroutines.io.ByteReadChannel"
    )
)
fun ByteReadChannel(text: String, charset: Charset = Charsets.UTF_8): ByteReadChannel =
    kotlinx.coroutines.io.ByteReadChannel(text, charset)

@Suppress("DEPRECATION")
@Deprecated(
    "Use scope.reader instead",
    ReplaceWith(
        "kotlinx.coroutines.io.reader(coroutineContext, channel, parent, block)",
        "kotlinx.coroutines.io.reader"
    )
)
fun reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = kotlinx.coroutines.io.reader(coroutineContext, channel, parent, block)

@Suppress("DEPRECATION")
@Deprecated(
    "Use scope.reader instead",
    ReplaceWith(
        "kotlinx.coroutines.io.reader(coroutineContext, autoFlush, parent, block)",
        "kotlinx.coroutines.io.reader"
    )
)
fun reader(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob = kotlinx.coroutines.io.reader(coroutineContext, autoFlush, parent, block)

@Suppress("DEPRECATION")
@Deprecated(
    "Use scope.writer instead",
    ReplaceWith(
        "kotlinx.coroutines.io.writer(coroutineContext, channel, parent, block)",
        "kotlinx.coroutines.io.writer"
    )
)
fun writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob = kotlinx.coroutines.io.writer(coroutineContext, channel, parent, block)

@Suppress("DEPRECATION")
@Deprecated(
    "Use scope.writer instead",
    ReplaceWith(
        "kotlinx.coroutines.io.writer(coroutineContext, autoFlush, parent, block)",
        "kotlinx.coroutines.io.writer"
    )
)
fun writer(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob = kotlinx.coroutines.io.writer(coroutineContext, autoFlush, parent, block)

