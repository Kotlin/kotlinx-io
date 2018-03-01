package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy

actual class BufferView internal constructor(
        private var content: CPointer<ByteVar>,
        private val contentCapacity: Int,
        internal actual val origin: BufferView?
) {
    private var refCount = 1

    private var readPosition = 0
    private var writePosition = 0
    private var limit = contentCapacity

    private var platformEndian = ByteOrder.BIG_ENDIAN === ByteOrder.nativeOrder()

    actual var attachment: Any? = null
    actual var next: BufferView? = null

    actual val capacity: Int get() = contentCapacity
    actual val readRemaining: Int get() = writePosition - readPosition
    actual val writeRemaining: Int get() = limit - writePosition

    actual fun canRead() = writePosition > readPosition
    actual fun canWrite() = writePosition < limit

    actual var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            field = newOrder
            platformEndian = newOrder === ByteOrder.nativeOrder()
        }

    actual fun reserveStartGap(n: Int) {
        if (readPosition > 0) throw IllegalStateException("Start gap is already reserved")
        if (writePosition > 0) throw IllegalStateException("Start gap is already reserved")
        writePosition = n
        readPosition = n
    }

    actual fun reserveEndGap(n: Int) {
        if (limit != contentCapacity) throw IllegalStateException("End gap is already reserved")
        limit -= n
    }

    actual val startGap: Int get() = readPosition
    actual val endGap: Int get() = contentCapacity - limit


    actual fun readByte(): Byte {
        if (readRemaining < 0) throw IllegalStateException("No bytes available for read")
        val value = content[readPosition]
        readPosition++
        return value
    }

    actual fun writeByte(v: Byte) {
        if (writeRemaining < 1) throw IllegalStateException("No space left for writing")
        content[writePosition] = v
        writePosition++
    }

    actual fun readShort(): Short {
        if (readRemaining < 2) throw IllegalStateException("Not enough bytes available to read a short")
        var value = (content + readPosition)!!.reinterpret<ShortVar>()[0]
        if (!platformEndian) value = swap(value)
        readPosition += 2
        return value
    }

    actual fun writeShort(v: Short) {
        if (writeRemaining < 2) throw IllegalStateException("Not enough space left to write a short")
        var value = v
        if (!platformEndian) value = swap(value)
        (content + writePosition)!!.reinterpret<ShortVar>()[0] = value
        writePosition += 2
    }

    actual fun readInt(): Int {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read an int")
        var value = (content + readPosition)!!.reinterpret<IntVar>()[0]
        if (!platformEndian) value = swap(value)
        readPosition += 4
        return value
    }

    actual fun writeInt(v: Int) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write an int")
        var value = if (platformEndian) v else swap(v)
        (content + writePosition)!!.reinterpret<IntVar>()[0] = value
        writePosition += 4
    }

    actual fun readFloat(): Float {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read a float")
        return memScoped {
            val value = alloc<IntVar>()
            value.value = (content + readPosition)!!.reinterpret<IntVar>()[0]
            if (!platformEndian) value.value = swap(value.value)
            readPosition += 4
            value.reinterpret<FloatVar>().value
        }
    }

    actual fun writeFloat(v: Float) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write a float")
        memScoped {
            val value = alloc<IntVar>()
            value.reinterpret<FloatVar>().value = v
            if (!platformEndian) value.value = swap(value.value)
            (content + writePosition)!!.reinterpret<IntVar>()[0] = value.value
        }
        writePosition += 4
    }
    
    actual fun readDouble(): Double {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a double")
        if (platformEndian) {
            val v = (content + readPosition)!!.reinterpret<DoubleVar>()[0]
            readPosition += 8
            return v
        }

        return memScoped {
            val ints = (content + readPosition)!!.reinterpret<IntVar>()
            val value = allocArray<DoubleVar>(1)
            val rints = value.reinterpret<IntVar>()

            rints[0] = swap(ints[1])
            rints[1] = swap(ints[0])

            readPosition += 8

            value[0]
        }
    }

    actual fun writeDouble(v: Double) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough bytes available to write a double")

        if (platformEndian) {
            (content + writePosition)!!.reinterpret<DoubleVar>()[0] = v
        } else {
            memScoped {
                val value = allocArray<IntVar>(2)
                value.reinterpret<DoubleVar>()[0] = v

                (content + writePosition)!!.reinterpret<IntVar>()[0] = swap(value[1])
                (content + writePosition)!!.reinterpret<IntVar>()[1] = swap(value[0])
            }
        }   

        writePosition += 8
    }

    fun read(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= readRemaining)
        require(length >= 0)
        
        memcpy(dst + offset, content + readPosition, length.toLong())
        readPosition += length
    }

    fun write(array: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= writeRemaining)
        require(length >= 0)

        memcpy(content + writePosition, array + offset, length.toLong())
        writePosition += length
    }

    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        require(length <= readRemaining)
        require(length >= 0)
        require(offset >= 0)
        require(offset + length <= dst.size)

        memScoped {
            val array = allocArray<ByteVar>(length)
            memcpy(array, content + readPosition, length.toLong())
            for (i in 0 .. length - 1) {
                dst[i + offset] = array[i]
            }
        }
        readPosition += length
    }

    actual fun write(array: ByteArray, offset: Int, length: Int) {
        require(length <= writeRemaining)
        require(length >= 0)
        require(offset >= 0)
        require(offset + length <= array.size)

        memScoped {
            val tmp = allocArray<ByteVar>(length)
            for (i in 0 .. length - 1) {
                tmp[i] = array[i + offset]
            }
            memcpy(content + writePosition, tmp, length.toLong())
        }

        writePosition += length
    }

    actual fun readLong(): Long {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a long")
        val m = 0xffffffff
        val a = readInt().toLong() and m
        val b = readInt().toLong() and m

        return if (byteOrder === ByteOrder.LITTLE_ENDIAN) {
            (b shl 32) or a
        } else {
            (a shl 32) or b
        }
    }

    actual fun writeLong(v: Long) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough space left to write a long")
        val m = 0xffffffff
        val a = (v shr 32).toInt()
        val b = (v and m).toInt()

        if (byteOrder === ByteOrder.LITTLE_ENDIAN) {
            writeInt(b)
            writeInt(a)
        } else {
            writeInt(a)
            writeInt(b)
        }
    }

    actual fun writeBuffer(src: BufferView, length: Int): Int {
        require(length <= src.readRemaining) { "length is too large: not enough bytes to read $length > ${src.readRemaining}"}
        require(length <= writeRemaining) { "length is too large: not enough room to write $length > $writeRemaining" }

        memcpy(content + writePosition, src.content + src.readPosition, length.toLong())

        src.readPosition += length
        writePosition += length

        return length
    }

    internal actual fun writeBufferPrepend(other: BufferView) {
        val size = other.readRemaining
        require(size <= startGap) { "size should be greater than startGap (size = $size, startGap = $startGap)" }

        memcpy(content + (readPosition - size), other.content + readPosition, size.toLong())

        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: BufferView, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap) { "should should be greater than write space + end gap (size = $size, " +
                "writeRemaining = $writeRemaining, endGap = $endGap, rem+gap = ${writeRemaining + endGap}" }


        memcpy(content + writePosition, other.content + readPosition, size.toLong())

        writePosition += size
        if (writePosition > limit) {
            limit = writePosition
        }
        other.readPosition += size
    }

    actual fun discardExact(n: Int) {
        val rem = readRemaining
        if (n > rem) throw IllegalArgumentException("Can't discard $n bytes: only $rem bytes available")
        readPosition += n
    }


    actual fun resetForWrite() {
        resetForWrite(contentCapacity)
    }

    actual fun resetForWrite(limit: Int) {
        require(limit <= contentCapacity)
        readPosition = 0
        writePosition = 0
        this.limit = limit
    }

    actual fun resetForRead() {
        readPosition = 0
        limit = contentCapacity
        writePosition = limit
    }

    actual fun pushBack(n: Int) {
        if (readPosition == 0) throw IllegalStateException("Nothing to push back")
        readPosition--
    }

    internal fun unlink() {
        if (refCount != 0) throw IllegalStateException("Unable to unlink buffers: buffer view is in use")
        content = EmptyBuffer
        resetForWrite(0)
    }

    private fun acquire() {
        val v = refCount
        if (v == 0) throw IllegalStateException("Failed to acquire buffer: buffer has been already released")
        refCount = v + 1
    }

    private fun release(): Boolean {
        if (this === Empty) throw IllegalStateException("attempted to release BufferView.Empty")

        val v = refCount
        if (v == 0) throw IllegalStateException("Unable to release: buffer has been already released")
        val newCount = v - 1
        refCount = newCount
        return newCount == 0
    }

    actual fun isExclusivelyOwned(): Boolean = refCount == 1

    actual fun makeView(): BufferView {
        val o = origin ?: this
        o.acquire()

        val view = BufferView(content, contentCapacity, o)
        view.attachment = attachment
        view.readPosition = readPosition
        view.writePosition = writePosition
        view.limit = limit

        return view
    }

    actual fun release(pool: ObjectPool<BufferView>) {
        if (release()) {
            resetForWrite()

            if (origin != null) {
                unlink()
                origin.release(pool)
            } else {
                pool.recycle(this)
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Short): Short = (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()
    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Int): Int = (swap((s and 0xffff).toShort()).toInt() shl 16) or (swap((s ushr 16).toShort()).toInt() and 0xffff)

    actual companion object {
        private val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        actual val Empty = BufferView(EmptyBuffer, 0, null)

        actual val Pool: ObjectPool<BufferView> = object: DefaultPool<BufferView>(BUFFER_VIEW_POOL_SIZE) {
            override fun produceInstance(): BufferView {
                val buffer = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
                return BufferView(buffer, BUFFER_VIEW_SIZE, null)
            }

            override fun clearInstance(instance: BufferView): BufferView {
                return super.clearInstance(instance).apply {
                    instance.resetForWrite()
                    instance.next = null
                    instance.attachment = null

                    if (instance.refCount != 0) throw IllegalStateException("Unable to clear instance: refCount is ${instance.refCount} != 0")
                    instance.refCount = 1
                }
            }

            override fun validateInstance(instance: BufferView) {
                super.validateInstance(instance)

                require(instance.refCount == 0) { "unable to recycle buffer: buffer view is in use (refCount = ${instance.refCount})"}
                require(instance.origin == null) { "Unable to recycle buffer view: view copy shouldn't be recycled" }
            }

            override fun disposeInstance(instance: BufferView) {
                require(instance.refCount == 0)
                require(instance.content !== EmptyBuffer)

                nativeHeap.free(instance.content)
            }
        }

        actual val NoPool: ObjectPool<BufferView> = object : NoPoolImpl<BufferView>() {
            override fun borrow(): BufferView {
                val content = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
                return BufferView(content, BUFFER_VIEW_SIZE, null)
            }

            override fun recycle(instance: BufferView) {
                require(instance.refCount == 0)
                require(instance.content !== EmptyBuffer)
                nativeHeap.free(instance.content)
            }
        }
    }
}
