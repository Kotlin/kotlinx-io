package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlin.test.*

class BufferExceptionsTest {
    // Note: Buffer on K/N is long-addressed
    @Test
    fun testOutOfBoundsOperator() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[0] = 1 }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[0] }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[Long.MAX_VALUE / 2 + 1] }
    }

    @Test
    fun testOutOfBoundsLoadDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsStoreDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(-1, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(-1L, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(Long.MAX_VALUE / 2 + 1, 0.0) }
    }

    @Test
    fun testOutOfBoundsStoreFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(-1, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(-1L, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(Long.MAX_VALUE / 2 + 1, 0f) }
    }

    @Test
    fun testOutOfBoundsStoreIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(-1L, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun testOutOfBoundsStoreLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(-1, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(-1L, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(Long.MAX_VALUE / 2 + 1, 0L) }
    }

    @Test
    fun testOutOfBoundsStoreShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(-1L, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun testOutOfBoundsStoreUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(Long.MAX_VALUE / 2 + 1, 0u) }

    }

    @Test
    fun testOutOfBoundsStoreUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun testOutOfBoundsStoreULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun testOutOfBoundsStoreUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }
}