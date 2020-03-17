package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.test.*

class CustomPoolTest {

    @Test
    fun testCustomPools() {
        val inputBuffer = bufferOf(ByteArray(10))
        val inputPool = SingleShotPool(inputBuffer)
        val outputBuffer = bufferOf(ByteArray(10))

        val input = object : Input(inputPool) {
            override fun closeSource() {
            }

            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertTrue { buffer.sameAs(outputBuffer) }
                buffer.storeByteAt(startIndex, 42)
                return 1
            }
        }

        val outputPool = SingleShotPool(outputBuffer)

        val output = object : Output(outputPool) {
            override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
                assertTrue(source.sameAs(outputBuffer))
                assertTrue(endIndex == 1)
            }

            override fun closeSource() {
            }
        }

        input.readAvailableTo(output)
        output.flush()

        input.close()
        output.close()
    }
}
