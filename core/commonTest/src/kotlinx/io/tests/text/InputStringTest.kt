package kotlinx.io.tests.text

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlinx.io.pool.*
import kotlinx.io.tests.utility.*
import kotlin.test.*

open class InputStringTest {
    open val pool: VerifyingObjectPool<Buffer> = VerifyingObjectPool(DirectAllocationBufferPool())

    @AfterTest
    fun verifyPool() {
        pool.assertEmpty()
    }

    @Test
    fun testReadLineSingleBuffer() {
        val p = buildBytes {
            writeUTF8String("1\r22\n333\r\n4444")
        }.asInput()

        assertEquals("1", p.readUTF8Line())
        assertEquals("22", p.readUTF8Line())
        assertEquals("333", p.readUTF8Line())
        assertEquals("4444", p.readUTF8Line())
        assertNull(p.readUTF8Line())
    }

}

private fun Input.readUTF8Line(): String {
    TODO()
}

private fun Output.writeUTF8String(text: String) {
    
}
