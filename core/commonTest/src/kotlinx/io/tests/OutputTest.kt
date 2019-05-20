package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.*

class OutputTest {
    @Test
    fun buildBytes() {
        val bytes = buildBytes {
            writeLong(0x0001020304050607)
            writeLong(0x08090A0B0C0D0E0F)
        }
        bytes.asInput().apply { 
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
        }
    }
}