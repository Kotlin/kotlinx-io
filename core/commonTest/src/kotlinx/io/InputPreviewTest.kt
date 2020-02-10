package kotlinx.io

import kotlin.test.*

class InputPreviewTest {
    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128
    private val prefetchSizes = (1..256)

    @Test
    fun testPreviewFromTheBeginning() = withInput {
        preview {
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun testPreviewAfterRead() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun testPreviewNested() = withInput {
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
    fun testPreviewSequential() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
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
    fun previewInterleaved() = withInput {
        assertReadLong(0x0001020304050607)
        preview {
            assertReadLong(0x08090A0B0C0D0E0F)
        }
        assertReadLong(0x08090A0B0C0D0E0F)
        preview {
            assertReadLong(0x1011121314151617)
        }
        assertReadLong(0x1011121314151617)
    }

    @Test
    fun testPreviewSequentialLonger() = withInput {
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

    private fun withInput(body: Input.() -> Unit) {
        prefetchSizes.forEach { prefetchSize ->
            bufferSizes.forEach { size ->
                val input = sequentialInfiniteInput(fetchSizeLimit, size)
                assertTrue(input.prefetch(prefetchSize), "Can't prefetch bytes")
                input.body()
            }
        }
    }
}
