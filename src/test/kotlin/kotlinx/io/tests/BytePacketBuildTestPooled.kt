package kotlinx.io.tests

import kotlinx.io.core.*

class BytePacketBuildTestPooled : BytePacketBuildTest() {
    override val pool = VerifyingObjectPool(BufferView.Pool)
}
