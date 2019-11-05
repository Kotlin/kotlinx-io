package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlinx.io.tests.*
import kotlin.test.*

class BufferExceptionsTest {
    // Note: Buffer on K/N is long-addressed
    @Test
    fun outOfBoundsOperator() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[0] = 1 }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[0] }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY[Long.MAX_VALUE / 2 + 1] }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY[Long.MAX_VALUE / 2 + 1] }
        }
    }

    @Test
    fun outOfBoundsLoadDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadDoubleAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadDoubleAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadFloatAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadFloatAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadIntAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadIntAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadLongAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadLongAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadShortAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadShortAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUByteAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadUByteAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUIntAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadUIntAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadULongAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadULongAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsLoadUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(-1L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.loadUShortAt(Long.MAX_VALUE / 2 + 1) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.loadUShortAt(Long.MAX_VALUE / 2 + 1) }
        }
    }

    @Test
    fun outOfBoundsStoreDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(-1, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(-1L, 0.0) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeDoubleAt(Long.MAX_VALUE / 2 + 1, 0.0) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeDoubleAt(Long.MAX_VALUE / 2 + 1, 0.0) }
        }
    }

    @Test
    fun outOfBoundsStoreFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(-1, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(-1L, 0f) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeFloatAt(Long.MAX_VALUE / 2 + 1, 0f) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeFloatAt(Long.MAX_VALUE / 2 + 1, 0f) }
        }
    }

    @Test
    fun outOfBoundsStoreIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(-1L, 0) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeIntAt(Long.MAX_VALUE / 2 + 1, 0) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeIntAt(Long.MAX_VALUE / 2 + 1, 0) }
        }
    }

    @Test
    fun outOfBoundsStoreLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(-1, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(-1L, 0L) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeLongAt(Long.MAX_VALUE / 2 + 1, 0L) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeLongAt(Long.MAX_VALUE / 2 + 1, 0L) }
        }
    }

    @Test
    fun outOfBoundsStoreShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(-1L, 0) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeShortAt(Long.MAX_VALUE / 2 + 1, 0) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeShortAt(Long.MAX_VALUE / 2 + 1, 0) }
        }
    }

    @Test
    fun outOfBoundsStoreUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(-1L, 0u) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUByteAt(Long.MAX_VALUE / 2 + 1, 0u) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeUByteAt(Long.MAX_VALUE / 2 + 1, 0u) }
        }
    }

    @Test
    fun outOfBoundsStoreUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(-1L, 0u) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUIntAt(Long.MAX_VALUE / 2 + 1, 0u) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeUIntAt(Long.MAX_VALUE / 2 + 1, 0u) }
        }
    }

    @Test
    fun outOfBoundsStoreULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(-1L, 0u) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeULongAt(Long.MAX_VALUE / 2 + 1, 0u) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeULongAt(Long.MAX_VALUE / 2 + 1, 0u) }
        }
    }

    @Test
    fun outOfBoundsStoreUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(-1L, 0u) }
        if (isNative) {
            assertFailsWith<IndexOutOfBoundsException> { Buffer.EMPTY.storeUShortAt(Long.MAX_VALUE / 2 + 1, 0u) }
        } else {
            assertFailsWith<IllegalArgumentException> { Buffer.EMPTY.storeUShortAt(Long.MAX_VALUE / 2 + 1, 0u) }
        }
    }
}