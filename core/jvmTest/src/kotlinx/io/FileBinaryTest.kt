package kotlinx.io

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FileBinaryTest {

    val bytes = ByteArray(128) { it.toByte() }

    val file = Files.createTempFile("kotlinx-io", ".test").apply {
        Files.write(this, bytes)
    }

    val binary = file.asBinary()

    @Test
    fun testFullFileRead() {
        binary.read {
            discard(3)
            assertReadByte(3)
            assertFails {
                discardExact(128)
            }
        }
    }

    @Test
    fun testStandAloneBinary() {
        val sub = binary.read {
            discard(10)
            readBinary(20)
        }

        assertEquals(20, sub.size)

        sub.read {
            readByteArray(20)
            assertFails {
                readByte()
            }
        }

        sub.read {
            val res = readByteArray(10)
            assertEquals(10, res[0])
        }
    }
}