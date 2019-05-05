package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.*

class InputPreviewTest {
    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128

    fun withInput(body: Input.() -> Unit) = bufferSizes.forEach { size ->
        sequentialInfiniteInput(fetchSizeLimit, size).apply(body)
    }

    @Test
    fun `Preview from the beginning`() = withInput {
        preview {
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun `Preview after read`() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun `Preview nested`() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
            preview {
                assertReadLong(0x1011121314151617)
            }
            assertReadLong(0x1011121314151617)
        }
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun `Preview sequential`() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun `Preview sequential longer`() = withInput {
        preview {
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
            assertReadLong(0x1011121314151617)
        }
        preview {
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
            assertReadLong(0x1011121314151617)
        }
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }
}

private fun Input.assertReadLong(expected: Long) {
    val value = readLong()
    if (value == expected)
        return

    fail("Expected: ${expected.toString(16).padStart(16, '0')}, actual: ${value.toString(16).padStart(16, '0')}")
}

private fun Long.printit(): Long {
    println(toString(16))
    return this
}
