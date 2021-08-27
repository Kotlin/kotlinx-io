package kotlinx.io.buffer

import kotlin.test.Test
import kotlin.test.assertFailsWith


class BufferExceptionsTest {
    // Note: Buffer on K/N is long-addressed
    @Test
    fun testOutOfBoundsOperator() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY[0] = 1 }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY[0] }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY[Long.MAX_VALUE / 2 + 1] }
    }

    @Test
    fun testOutOfBoundsLoadDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadDoubleAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadDoubleAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadDoubleAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadFloatAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadFloatAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadFloatAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadIntAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadLongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadLongAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadLongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadShortAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUByteAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUByteAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUByteAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUIntAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadULongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadULongAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadULongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsLoadUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUShortAt(-1L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.loadUShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun testOutOfBoundsStoreDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeDoubleAt(-1, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeDoubleAt(-1L, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeDoubleAt(Long.MAX_VALUE / 2 + 1, 0.0) }
    }

    @Test
    fun testOutOfBoundsStoreFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeFloatAt(-1, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeFloatAt(-1L, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeFloatAt(Long.MAX_VALUE / 2 + 1, 0f) }
    }

    @Test
    fun testOutOfBoundsStoreIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeIntAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeIntAt(-1L, 0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeIntAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun testOutOfBoundsStoreLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeLongAt(-1, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeLongAt(-1L, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeLongAt(Long.MAX_VALUE / 2 + 1, 0L) }
    }

    @Test
    fun testOutOfBoundsStoreShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeShortAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeShortAt(-1L, 0) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeShortAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun testOutOfBoundsStoreUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUByteAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUByteAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUByteAt(Long.MAX_VALUE / 2 + 1, 0u) }

    }

    @Test
    fun testOutOfBoundsStoreUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUIntAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUIntAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUIntAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun testOutOfBoundsStoreULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeULongAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeULongAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeULongAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun testOutOfBoundsStoreUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUShortAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUShortAt(-1L, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { EMPTY.storeUShortAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }
}
