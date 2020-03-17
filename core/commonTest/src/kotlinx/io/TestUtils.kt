package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*
import kotlinx.io.utils.LeakDetectingPool
import kotlin.test.*

fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.size, actual.size, "Expected array lengths to be equal")
    assertEquals(expected.toHexString(), actual.toHexString())
}

fun ByteArray.toHexString(): String = "0x" + joinToString("") {
    it.toUInt().toString(16).padStart(2, '0')
}

public fun StringInput(string: String) = ByteArrayInput(string.encodeToByteArray())

public open class LeakDetector {
    protected lateinit var pool: ObjectPool<Buffer>

    @BeforeTest
    fun before() {
        pool = LeakDetectingPool()
    }

    @AfterTest
    fun after() {
        pool.close()
    }
}
