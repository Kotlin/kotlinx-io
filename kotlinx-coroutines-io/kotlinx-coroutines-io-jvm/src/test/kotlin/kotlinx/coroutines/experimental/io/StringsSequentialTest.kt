package kotlinx.coroutines.experimental.io

import kotlinx.io.core.*

class StringsSequentialTest : StringsTest() {
    override fun ByteChannel(autoFlush: Boolean): ByteChannel {
        return ByteChannelSequentialJVM(IoBuffer.Empty, autoFlush)
    }
}
