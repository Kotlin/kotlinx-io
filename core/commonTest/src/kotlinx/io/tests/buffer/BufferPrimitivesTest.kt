package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlin.test.*

class BufferPrimitivesTest {
    @Test
    fun storeAndLoadDouble() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeDoubleAt(0, 1.0)
        buffer.storeDoubleAt(8L, 2.0)
        assertEquals(1.0, buffer.loadDoubleAt(0))
        assertEquals(1.0, buffer.loadDoubleAt(0L))
        assertEquals(2.0, buffer.loadDoubleAt(8))
        assertEquals(2.0, buffer.loadDoubleAt(8L))
    }

    @Test
    fun storeCopyAndLoadDouble() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeDoubleAt(0, 1.0)
        buffer.storeDoubleAt(8L, 2.0)
        buffer.storeDoubleAt(16L, 3.0)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1.0, buffer.loadDoubleAt(0))
        assertEquals(2.0, buffer.loadDoubleAt(8))
        assertEquals(3.0, buffer.loadDoubleAt(16))
        assertEquals(1.0, copy.loadDoubleAt(0))
        assertEquals(2.0, copy.loadDoubleAt(8))
        assertEquals(0.0, copy.loadDoubleAt(16))
    }

    @Test
    fun storeAndLoadFloat() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeFloatAt(0, 1f)
        buffer.storeFloatAt(8L, 2f)
        assertEquals(1f, buffer.loadFloatAt(0))
        assertEquals(1f, buffer.loadFloatAt(0L))
        assertEquals(2f, buffer.loadFloatAt(8))
        assertEquals(2f, buffer.loadFloatAt(8L))
    }

    @Test
    fun storeCopyAndLoadFloat() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeFloatAt(0, 1f)
        buffer.storeFloatAt(8L, 2f)
        buffer.storeFloatAt(16L, 3f)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1f, buffer.loadFloatAt(0))
        assertEquals(2f, buffer.loadFloatAt(8))
        assertEquals(3f, buffer.loadFloatAt(16))
        assertEquals(1f, copy.loadFloatAt(0))
        assertEquals(2f, copy.loadFloatAt(8))
        assertEquals(0f, copy.loadFloatAt(16))
    }

    @Test
    fun storeAndLoadInt() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeIntAt(0, 1)
        buffer.storeIntAt(8L, 2)
        assertEquals(1, buffer.loadIntAt(0))
        assertEquals(1, buffer.loadIntAt(0L))
        assertEquals(2, buffer.loadIntAt(8))
        assertEquals(2, buffer.loadIntAt(8L))
    }

    @Test
    fun storeCopyAndLoadInt() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeIntAt(0, 1)
        buffer.storeIntAt(8L, 2)
        buffer.storeIntAt(16L, 3)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1, buffer.loadIntAt(0))
        assertEquals(2, buffer.loadIntAt(8))
        assertEquals(3, buffer.loadIntAt(16))
        assertEquals(1, copy.loadIntAt(0))
        assertEquals(2, copy.loadIntAt(8))
        assertEquals(0, copy.loadIntAt(16))
    }

    @Test
    fun storeAndLoadLong() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeLongAt(0, 1L)
        buffer.storeLongAt(8L, 2L)
        assertEquals(1L, buffer.loadLongAt(0))
        assertEquals(1L, buffer.loadLongAt(0L))
        assertEquals(2L, buffer.loadLongAt(8))
        assertEquals(2L, buffer.loadLongAt(8L))
    }

    @Test
    fun storeCopyAndLoadLong() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeLongAt(0, 1L)
        buffer.storeLongAt(8L, 2L)
        buffer.storeLongAt(16L, 3L)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1L, buffer.loadLongAt(0))
        assertEquals(2L, buffer.loadLongAt(8))
        assertEquals(3L, buffer.loadLongAt(16))
        assertEquals(1L, copy.loadLongAt(0))
        assertEquals(2L, copy.loadLongAt(8))
        assertEquals(0L, copy.loadLongAt(16))
    }

    @Test
    fun storeAndLoadShort() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeShortAt(0, 1)
        buffer.storeShortAt(8L, 2)
        assertEquals(1, buffer.loadShortAt(0))
        assertEquals(1, buffer.loadShortAt(0L))
        assertEquals(2, buffer.loadShortAt(8))
        assertEquals(2, buffer.loadShortAt(8L))
    }

    @Test
    fun storeCopyAndLoadShort() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeShortAt(0, 1)
        buffer.storeShortAt(8L, 2)
        buffer.storeShortAt(16L, 3)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1, buffer.loadShortAt(0))
        assertEquals(2, buffer.loadShortAt(8))
        assertEquals(3, buffer.loadShortAt(16))
        assertEquals(1, copy.loadShortAt(0))
        assertEquals(2, copy.loadShortAt(8))
        assertEquals(0, copy.loadShortAt(16))
    }

    @Test
    fun storeAndLoadUByte() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeUByteAt(0, 1u)
        buffer.storeUByteAt(8L, 2u)
        assertEquals(1u, buffer.loadUByteAt(0))
        assertEquals(1u, buffer.loadUByteAt(0L))
        assertEquals(2u, buffer.loadUByteAt(8))
        assertEquals(2u, buffer.loadUByteAt(8L))
    }

    @Test
    fun storeCopyAndLoadUByte() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeUByteAt(0, 1u)
        buffer.storeUByteAt(8L, 2u)
        buffer.storeUByteAt(16L, 3u)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1u, buffer.loadUByteAt(0))
        assertEquals(2u, buffer.loadUByteAt(8))
        assertEquals(3u, buffer.loadUByteAt(16))
        assertEquals(1u, copy.loadUByteAt(0))
        assertEquals(2u, copy.loadUByteAt(8))
        assertEquals(0u, copy.loadUByteAt(16))
    }

    @Test
    fun storeAndLoadUInt() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeUIntAt(0, 1u)
        buffer.storeUIntAt(8L, 2u)
        assertEquals(1u, buffer.loadUIntAt(0))
        assertEquals(1u, buffer.loadUIntAt(0L))
        assertEquals(2u, buffer.loadUIntAt(8))
        assertEquals(2u, buffer.loadUIntAt(8L))
    }

    @Test
    fun storeCopyAndLoadUInt() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeUIntAt(0, 1u)
        buffer.storeUIntAt(8L, 2u)
        buffer.storeUIntAt(16L, 3u)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1u, buffer.loadUIntAt(0))
        assertEquals(2u, buffer.loadUIntAt(8))
        assertEquals(3u, buffer.loadUIntAt(16))
        assertEquals(1u, copy.loadUIntAt(0))
        assertEquals(2u, copy.loadUIntAt(8))
        assertEquals(0u, copy.loadUIntAt(16))
    }

    @Test
    fun storeAndLoadULong() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeULongAt(0, 1u)
        buffer.storeULongAt(8L, 2u)
        assertEquals(1u, buffer.loadULongAt(0))
        assertEquals(1u, buffer.loadULongAt(0L))
        assertEquals(2u, buffer.loadULongAt(8))
        assertEquals(2u, buffer.loadULongAt(8L))
    }

    @Test
    fun storeCopyAndLoadULong() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeULongAt(0, 1u)
        buffer.storeULongAt(8L, 2u)
        buffer.storeULongAt(16L, 3u)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1u, buffer.loadULongAt(0))
        assertEquals(2u, buffer.loadULongAt(8))
        assertEquals(3u, buffer.loadULongAt(16))
        assertEquals(1u, copy.loadULongAt(0))
        assertEquals(2u, copy.loadULongAt(8))
        assertEquals(0u, copy.loadULongAt(16))
    }

    @Test
    fun storeAndLoadUShort() {
        val buffer = PlatformBufferAllocator.allocate(16)
        buffer.storeUShortAt(0, 1u)
        buffer.storeUShortAt(8L, 2u)
        assertEquals(1u, buffer.loadUShortAt(0))
        assertEquals(1u, buffer.loadUShortAt(0L))
        assertEquals(2u, buffer.loadUShortAt(8))
        assertEquals(2u, buffer.loadUShortAt(8L))
    }

    @Test
    fun storeCopyAndLoadUShort() {
        val buffer = PlatformBufferAllocator.allocate(24)
        buffer.storeUShortAt(0, 1u)
        buffer.storeUShortAt(8L, 2u)
        buffer.storeUShortAt(16L, 3u)
        val copy = PlatformBufferAllocator.allocate(24)
        buffer.copyTo(copy, 0, 16, 0)
        assertEquals(1u, buffer.loadUShortAt(0))
        assertEquals(2u, buffer.loadUShortAt(8))
        assertEquals(3u, buffer.loadUShortAt(16))
        assertEquals(1u, copy.loadUShortAt(0))
        assertEquals(2u, copy.loadUShortAt(8))
        assertEquals(0u, copy.loadUShortAt(16))
    }

// Test generator (works only on JVM):
//    fun main() {
//        val methods = Class.forName("kotlinx.io.buffer.PrimitivesOperationsKt")
//            .declaredMethods
//            .toList() +
//                Class.forName("kotlinx.io.buffer.JvmPrimitivesOperationsKt").declaredMethods
//                    .toList()
//
//        fun value(value: String, mn: String): String {
//            if (mn.contains("U")) return "${value}u"
//            if (mn.contains("Float")) return "${value}f"
//            if (mn.contains("Double")) return "${value}.0"
//            if (mn.contains("Long")) return "${value}L"
//            return "${value}"
//        }
//
//        methods.sortedBy { it.name }
//            .filter {
//                it.name.contains("store") && it.parameters[1].type != Long::class.java
//            }
//            .forEach {
//                val methodName =  it.name.substringBefore("-")
//                val load = methodName.replace("store", "load")
//                val testName = it.name.substringBefore("-").substringAfterLast("store").removeSuffix("At")
//                val zero = value("0", methodName)
//                val value = value("1", methodName)
//                val value2 = value("2", methodName)
//                val value3 = value("3", methodName)
//                println("""
//                @Test
//                fun storeAndLoad${testName.capitalize()}() {
//                    val buffer = PlatformBufferAllocator.allocate(16)
//                    buffer.$methodName(0, $value)
//                    buffer.$methodName(8L, $value2)
//                    assertEquals($value, buffer.$load(0))
//                    assertEquals($value, buffer.$load(0L))
//                    assertEquals($value2, buffer.$load(8))
//                    assertEquals($value2, buffer.$load(8L))
//                }
//
//                @Test
//                fun storeCopyAndLoad${testName.capitalize()}() {
//                    val buffer = PlatformBufferAllocator.allocate(24)
//                    buffer.$methodName(0, $value)
//                    buffer.$methodName(8L, $value2)
//                    buffer.$methodName(16L, $value3)
//                    val copy = PlatformBufferAllocator.allocate(24)
//                    buffer.copyTo(copy, 0, 16, 0)
//                    assertEquals($value, buffer.$load(0))
//                    assertEquals($value2, buffer.$load(8))
//                    assertEquals($value3, buffer.$load(16))
//                    assertEquals($value, copy.$load(0))
//                    assertEquals($value2, copy.$load(8))
//                    assertEquals($zero, copy.$load(16))
//                }
//            """.trimIndent())
//                println()
//            }
//    }
}
