package kotlinx.coroutines.experimental.io

import kotlinx.io.core.*

class ByteBufferChannelScenarioSequentialTest : ByteBufferChannelScenarioTest() {

    override fun ByteChannel(autoFlush: Boolean): ByteChannel {
        return ByteChannelSequentialJVM(IoBuffer.Empty, autoFlush)
    }
}
