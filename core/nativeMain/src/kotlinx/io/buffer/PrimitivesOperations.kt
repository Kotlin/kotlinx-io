@file:Suppress("ConstantConditionIf")

package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlin.experimental.*

public actual fun Buffer.loadByteAt(index: Long): Byte = array[assertIndex(index, 1)]

public actual fun Buffer.loadShortAt(offset: Int): Short = usePointer {
    assertIndex(offset, 2)
    val pointer = it.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<ShortVar>().pointed.value.toBigEndian()
    else loadShortSlowAt(pointer)
}

public actual fun Buffer.loadShortAt(offset: Long): Short = usePointer {
    assertIndex(offset.toInt(), 2)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) pointer.reinterpret<ShortVar>().pointed.value.toBigEndian()
    else loadShortSlowAt(pointer)
}

public actual fun Buffer.loadIntAt(offset: Int): Int = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) pointer.reinterpret<IntVar>().pointed.value.toBigEndian()
    else loadIntSlowAt(pointer)
}

public actual fun Buffer.loadIntAt(offset: Long): Int = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) pointer.reinterpret<IntVar>().pointed.value.toBigEndian()
    else loadIntSlowAt(pointer)
}

public actual fun Buffer.loadLongAt(offset: Int): Long = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) pointer.reinterpret<LongVar>().pointed.value.toBigEndian()
    else loadLongSlowAt(pointer)
}

public actual fun Buffer.loadLongAt(offset: Long): Long = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<LongVar>().pointed.value.toBigEndian()
    else loadLongSlowAt(pointer)
}

public actual fun Buffer.loadFloatAt(offset: Int): Float = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!

    return if (unalignedAccessSupported) Float.fromBits(pointer.reinterpret<IntVar>().pointed.value.toBigEndian())
    else loadFloatSlowAt(pointer)
}

public actual fun Buffer.loadFloatAt(offset: Long): Float = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) Float.fromBits(pointer.reinterpret<IntVar>().pointed.value.toBigEndian())
    else loadFloatSlowAt(pointer)
}

public actual fun Buffer.loadDoubleAt(offset: Int): Double = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) Double.fromBits(pointer.reinterpret<LongVar>().pointed.value.toBigEndian())
    else loadDoubleSlowAt(pointer)
}

public actual fun Buffer.loadDoubleAt(offset: Long): Double = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!
    return if (unalignedAccessSupported) Double.fromBits(pointer.reinterpret<LongVar>().pointed.value.toBigEndian())
    else loadDoubleSlowAt(pointer)
}

public actual fun Buffer.storeIntAt(offset: Int, value: Int) = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toBigEndian()
    } else {
        storeIntSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeIntAt(offset: Long, value: Int) = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toBigEndian()
    } else {
        storeIntSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeShortAt(offset: Int, value: Short) = usePointer {
    assertIndex(offset, 2)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
    } else {
        storeShortSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeShortAt(offset: Long, value: Short) = usePointer {
    assertIndex(offset, 2)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
    } else {
        storeShortSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeLongAt(offset: Int, value: Long) = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toBigEndian()
    } else {
        storeLongSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeLongAt(offset: Long, value: Long) = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!
    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toBigEndian()
    } else {
        storeLongSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeFloatAt(offset: Int, value: Float) = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toRawBits().toBigEndian()
    } else {
        storeFloatSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeFloatAt(offset: Long, value: Float) = usePointer {
    assertIndex(offset, 4)
    val pointer = it.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toRawBits().toBigEndian()
    } else {
        storeFloatSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeDoubleAt(offset: Int, value: Double) = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toRawBits().toBigEndian()
    } else {
        storeDoubleSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeDoubleAt(offset: Long, value: Double) = usePointer {
    assertIndex(offset, 8)
    val pointer = it.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toRawBits().toBigEndian()
    } else {
        storeDoubleSlowAt(pointer, value)
    }
}

public actual fun Buffer.storeByteAt(index: Long, value: Byte) = usePointer {
    it[assertIndex(index, 1)] = value
}

internal fun storeShortSlowAt(pointer: CPointer<ByteVar>, value: Short) {
    pointer[0] = (value.toInt() ushr 8).toByte()
    pointer[1] = (value and 0xff).toByte()
}

internal fun storeIntSlowAt(pointer: CPointer<ByteVar>, value: Int) {
    pointer[0] = (value ushr 24).toByte()
    pointer[1] = (value ushr 16).toByte()
    pointer[2] = (value ushr 8).toByte()
    pointer[3] = (value and 0xff).toByte()
}

internal fun storeLongSlowAt(pointer: CPointer<ByteVar>, value: Long) {
    pointer[0] = (value ushr 56).toByte()
    pointer[1] = (value ushr 48).toByte()
    pointer[2] = (value ushr 40).toByte()
    pointer[3] = (value ushr 32).toByte()
    pointer[4] = (value ushr 24).toByte()
    pointer[5] = (value ushr 16).toByte()
    pointer[6] = (value ushr 8).toByte()
    pointer[7] = (value and 0xff).toByte()
}

internal fun storeFloatSlowAt(pointer: CPointer<ByteVar>, value: Float) {
    storeIntSlowAt(pointer, value.toRawBits())
}

internal fun storeDoubleSlowAt(pointer: CPointer<ByteVar>, value: Double) {
    storeLongSlowAt(pointer, value.toRawBits())
}

internal fun loadShortSlowAt(pointer: CPointer<ByteVar>): Short {
    return ((pointer[0].toInt() shl 8) or (pointer[1].toInt() and 0xff)).toShort()
}

internal fun loadIntSlowAt(pointer: CPointer<ByteVar>): Int {
    return ((pointer[0].toInt() shl 24) or
            (pointer[1].toInt() shl 16) or
            (pointer[2].toInt() shl 18) or
            (pointer[3].toInt() and 0xff))
}

internal fun loadLongSlowAt(pointer: CPointer<ByteVar>): Long {
    return ((pointer[0].toLong() shl 56) or
            (pointer[1].toLong() shl 48) or
            (pointer[2].toLong() shl 40) or
            (pointer[3].toLong() shl 32) or
            (pointer[4].toLong() shl 24) or
            (pointer[5].toLong() shl 16) or
            (pointer[6].toLong() shl 8) or
            (pointer[7].toLong() and 0xffL))
}

internal fun loadFloatSlowAt(pointer: CPointer<ByteVar>): Float {
    return Float.fromBits(loadIntSlowAt(pointer))
}

internal fun loadDoubleSlowAt(pointer: CPointer<ByteVar>): Double {
    return Double.fromBits(loadLongSlowAt(pointer))
}
