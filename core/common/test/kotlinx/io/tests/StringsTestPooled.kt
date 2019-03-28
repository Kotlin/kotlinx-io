package kotlinx.io.tests

import kotlinx.io.core.internal.*

class StringsTestPooled : StringsTest() {
    override val pool: VerifyingObjectPool<ChunkBuffer> = VerifyingObjectPool(ChunkBuffer.Pool)
}
