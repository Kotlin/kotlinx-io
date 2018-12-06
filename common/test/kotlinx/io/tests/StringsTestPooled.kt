package kotlinx.io.tests

import kotlinx.io.core.*

class StringsTestPooled : StringsTest() {
    override val pool: VerifyingObjectPool<IoBuffer> = VerifyingObjectPool(IoBuffer.Pool)
}
