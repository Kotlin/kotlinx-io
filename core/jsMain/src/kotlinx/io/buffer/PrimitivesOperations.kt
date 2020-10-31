package kotlinx.io.buffer

import kotlinx.io.internal.toIntOrFail
import org.khronos.webgl.DataView
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public actual fun Buffer.loadByteAt(index: Long): Byte = loadByteAt(index.toIntOrFail("index"))

public actual fun Buffer.loadShortAt(offset: Int): Short = checked(offset) {
    return view.getInt16(offset, false)
}

public actual fun Buffer.loadShortAt(offset: Long): Short = checked(offset) {
    return loadShortAt(it)
}

public actual fun Buffer.loadIntAt(offset: Int): Int = checked(offset) {
    return view.getInt32(offset, false)
}

public actual fun Buffer.loadIntAt(offset: Long): Int = checked(offset) {
    return loadIntAt(it)
}

public actual fun Buffer.loadLongAt(offset: Int): Long = checked(offset) {
    return (view.getUint32(offset, false).toLong() shl 32) or
            view.getUint32(offset + 4, false).toLong()
}

public actual fun Buffer.loadLongAt(offset: Long): Long = checked(offset) {
    return loadLongAt(it)
}

public actual fun Buffer.loadFloatAt(offset: Int): Float = checked(offset) {
    return view.getFloat32(offset, false)
}

public actual fun Buffer.loadFloatAt(offset: Long): Float = checked(offset) {
    return loadFloatAt(it)
}

public actual fun Buffer.loadDoubleAt(offset: Int): Double = checked(offset) {
    return view.getFloat64(offset, false)
}

public actual fun Buffer.loadDoubleAt(offset: Long): Double = checked(offset) {
    return loadDoubleAt(it)
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeIntAt(offset: Int, value: Int): Unit = checked(offset) {
    view.setInt32(offset, value, littleEndian = false)
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeIntAt(offset: Long, value: Int): Unit = checked(offset) {
    view.setInt32(it, value, littleEndian = false)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeShortAt(offset: Int, value: Short): Unit = checked(offset) {
    view.setInt16(offset, value, littleEndian = false)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeShortAt(offset: Long, value: Short): Unit = checked(offset) {
    view.setInt16(it, value, littleEndian = false)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeLongAt(offset: Int, value: Long): Unit = checked(offset) {
    view.setInt32(offset, (value shr 32).toInt(), littleEndian = false)
    view.setInt32(offset + 4, (value and 0xffffffffL).toInt(), littleEndian = false)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
public actual fun Buffer.storeLongAt(offset: Long, value: Long): Unit = checked(offset) {
    storeLongAt(it, value)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
public actual fun Buffer.storeFloatAt(offset: Int, value: Float): Unit = checked(offset) {
    view.setFloat32(offset, value, littleEndian = false)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
public actual fun Buffer.storeFloatAt(offset: Long, value: Float): Unit = checked(offset) {
    view.setFloat32(it, value, littleEndian = false)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
public actual fun Buffer.storeDoubleAt(offset: Int, value: Double): Unit = checked(offset) {
    setFloat64(offset, value, littleEndian = false)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
public actual fun Buffer.storeDoubleAt(offset: Long, value: Double): Unit = checked(offset) {
    view.setFloat64(it, value, littleEndian = false)
}

public actual fun Buffer.storeByteAt(index: Long, value: Byte): Unit = checked(index) {
    storeByteAt(it, value)
}

@PublishedApi
internal inline fun <T> Buffer.checked(offset: Int, block: DataView.() -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return view.block()
    } catch (e: RangeError) {
        throw IndexOutOfBoundsException("Index: $offset, Size: $size")
    }
}

@PublishedApi
internal inline fun <T> Buffer.checked(offset: Long, block: DataView.(offset: Int) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        return view.block(offset.toIntOrFail("offset"))
    } catch (e: RangeError) {
        throw IndexOutOfBoundsException("Index: $offset, Size: $size")
    }
}

internal external class RangeError : Throwable
