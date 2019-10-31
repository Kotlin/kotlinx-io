package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlin.test.*

class BufferExceptionsTest {

    @Test
    fun outOfBoundsOperator() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty[0] = 1 }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty[0] }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty[Long.MAX_VALUE / 2 + 1] }
    }

    @Test
    fun outOfBoundsLoadDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadDoubleAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadDoubleAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadDoubleAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadFloatAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadFloatAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadFloatAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadIntAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadLongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadLongAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadLongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadShortAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUByteAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUByteAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadUByteAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUIntAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUIntAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadUIntAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadULongAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadULongAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadULongAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsLoadUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUShortAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.loadUShortAt(-1L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.loadUShortAt(Long.MAX_VALUE / 2 + 1) }
    }

    @Test
    fun outOfBoundsStoreDoubleAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeDoubleAt(-1, 0.0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeDoubleAt(-1L, 0.0) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeDoubleAt(Long.MAX_VALUE / 2 + 1, 0.0) }
    }

    @Test
    fun outOfBoundsStoreFloatAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeFloatAt(-1, 0f) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeFloatAt(-1L, 0f) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeFloatAt(Long.MAX_VALUE / 2 + 1, 0f) }
    }

    @Test
    fun outOfBoundsStoreIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeIntAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeIntAt(-1L, 0) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeIntAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun outOfBoundsStoreLongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeLongAt(-1, 0L) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeLongAt(-1L, 0L) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeLongAt(Long.MAX_VALUE / 2 + 1, 0L) }
    }

    @Test
    fun outOfBoundsStoreShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeShortAt(-1, 0) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeShortAt(-1L, 0) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeShortAt(Long.MAX_VALUE / 2 + 1, 0) }
    }

    @Test
    fun outOfBoundsStoreUByteAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUByteAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUByteAt(-1L, 0u) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeUByteAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun outOfBoundsStoreUIntAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUIntAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUIntAt(-1L, 0u) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeUIntAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun outOfBoundsStoreULongAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeULongAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeULongAt(-1L, 0u) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeULongAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }

    @Test
    fun outOfBoundsStoreUShortAt() {
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUShortAt(-1, 0u) }
        assertFailsWith<IndexOutOfBoundsException> { Buffer.Empty.storeUShortAt(-1L, 0u) }
        assertFailsWith<IllegalArgumentException> { Buffer.Empty.storeUShortAt(Long.MAX_VALUE / 2 + 1, 0u) }
    }
}