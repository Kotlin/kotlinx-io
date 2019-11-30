package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@ExperimentalIoApi
class InputToOutputTest {

    @Test
    fun simpleCopy() {
        buildBytes {
            repeat(100) {
                writeInt(it)
            }
        }.readIt { input ->
            buildBytes {
                writeInput(input)
            }.read {
                skipBytes(4 * 42)
                val res = readInt()
                assertEquals(42, res)
            }
        }
    }

    @Test
    fun limitedCopy() {
        buildBytes {
            repeat(1000) {
                writeInt(it)
            }
        }.readIt { input ->
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

    @Test
    fun toBytesArray() {
        val bytes = buildBytes {
            repeat(1000) {
                writeInt(it)
            }
        }

        val array = bytes.toByteArray()
        assertEquals(4000, array.size)
    }
}