package kotlinx.io.tests

import kotlinx.io.core.internal.*

class BytePacketBuildTestPooled : BytePacketBuildTest() {
    override val pool = VerifyingObjectPool(ChunkBuffer.Pool)
}
