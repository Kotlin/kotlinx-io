package kotlinx.coroutines.experimental.io

import kotlin.test.*

abstract class ByteChannelTestBase {
    protected val coroutines = DummyCoroutines()
    protected val ch: ByteChannel by lazy { ByteChannel(false) }
    protected val Size = 4096 - 8

    @AfterTest
    fun finish() {
        ch.close(CancellationException("Test finished"))
    }

    protected open fun ByteChannel(autoFlush: Boolean): ByteChannel {
        return kotlinx.coroutines.experimental.io.ByteChannel(autoFlush)
    }

    protected fun runTest(block: suspend () -> Unit) {
        coroutines.schedule(block)
        coroutines.run()
    }

    protected fun launch(block: suspend () -> Unit) {
        coroutines.schedule(block)
    }
}
