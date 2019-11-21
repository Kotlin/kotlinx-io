package kotlinx.io.tests

import kotlinx.io.buildBytes
import kotlinx.io.writeInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class InputToOutputTest {

    @Test
    fun simpleCopy() {
        val input = buildBytes {
            repeat(100) {
                writeInt(it)
            }
        }.input()

        buildBytes {
            writeInput(input)
        }.read {
            skipBytes(4 * 42)
            val res = readInt()
            assertEquals(42, res)
        }
    }

    @Test
    fun limitedCopy() {
        val input = buildBytes {
            repeat(1000) {
                writeInt(it)
            }
        }.input()

        buildBytes {
            val written = writeInput(input, 400)
            assertEquals(400, written)
        }.read {
            skipBytes(4 * 42)
            val res = readInt()
            assertEquals(42, res)
            assertFails {
                skipBytes(400 - 42 * 4)
            }
        }
    }
}