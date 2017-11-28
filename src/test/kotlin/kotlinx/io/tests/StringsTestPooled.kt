package kotlinx.io.tests

import kotlinx.io.core.*

class StringsTestPooled : StringsTest() {
    override val pool: VerifyingObjectPool<BufferView> = VerifyingObjectPool(BufferView.Pool)
}
