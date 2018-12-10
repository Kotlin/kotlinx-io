package kotlinx.coroutines.io

import kotlinx.io.core.*
import kotlin.math.*
import kotlin.test.*

open class ByteChannelSmokeTest : ByteChannelTestBase() {

    @Test
    fun simpleSmokeTest() {
        val bc = ByteChannel(false)
        bc.close()
    }

    @Test
    open fun testWriteInt() = runTest {
        val bc = ByteChannel(false)
        bc.writeInt(777)
        bc.flush()
        assertEquals(777, bc.readInt())
    }

    @Test
    fun testWriteLong() = runTest {
        val bc = ByteChannel(false)
        bc.writeLong(777)
        bc.flush()
        assertEquals(777L, bc.readLong())
    }

    @Test
    fun testReadLineClosed() = runTest {
        val bc = ByteChannel(true)
        bc.writeStringUtf8("Test")
        bc.close()

        val s = buildString {
            bc.readUTF8LineTo(this)
        }

        assertEquals("Test", s)
    }

    @Test
    fun testReadLine() = runTest {
        val bc = ByteChannel(true)
        bc.writeStringUtf8("Test\n")
        bc.flush()

        val s = buildString {
            bc.readUTF8LineTo(this)
        }

        assertEquals("Test", s)
    }

    @Test
    fun testBoolean() {
        runTest {
            ch.writeBoolean(true)
            ch.flush()
            assertEquals(true, ch.readBoolean())

            ch.writeBoolean(false)
            ch.flush()
            assertEquals(false, ch.readBoolean())
        }
    }

    @Test
    fun testByte() {
        runTest {
            assertEquals(0, ch.availableForRead)
            ch.writeByte(-1)
            ch.flush()
            assertEquals(1, ch.availableForRead)
            assertEquals(-1, ch.readByte())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testShortB() {
        runTest {
            ch.readByteOrder = ByteOrder.BIG_ENDIAN
            ch.writeByteOrder = ByteOrder.BIG_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeShort(-1)
            assertEquals(0, ch.availableForRead)
            ch.flush()
            assertEquals(2, ch.availableForRead)
            assertEquals(-1, ch.readShort())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testShortL() {
        runTest {
            ch.readByteOrder = ByteOrder.LITTLE_ENDIAN
            ch.writeByteOrder = ByteOrder.LITTLE_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeShort(-1)
            assertEquals(0, ch.availableForRead)
            ch.flush()
            assertEquals(2, ch.availableForRead)
            assertEquals(-1, ch.readShort())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testShortEdge() {
        runTest {
            ch.writeByte(1)

            for (i in 2 until Size step 2) {
                ch.writeShort(0x00ee)
            }

            ch.flush()

            ch.readByte()
            ch.writeShort(0x1234)

            ch.flush()

            while (ch.availableForRead > 2) {
                ch.readShort()
            }

            assertEquals(0x1234, ch.readShort())
        }
    }

    @Test
    fun testIntB() {
        runTest {
            ch.readByteOrder = ByteOrder.BIG_ENDIAN
            ch.writeByteOrder = ByteOrder.BIG_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeInt(-1)
            ch.flush()
            assertEquals(4, ch.availableForRead)
            assertEquals(-1, ch.readInt())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testIntL() {
        runTest {
            ch.readByteOrder = ByteOrder.LITTLE_ENDIAN
            ch.writeByteOrder = ByteOrder.LITTLE_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeInt(-1)
            ch.flush()
            assertEquals(4, ch.availableForRead)
            assertEquals(-1, ch.readInt())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testIntEdge() {
        runTest {
            for (shift in 1..3) {
                for (i in 1..shift) {
                    ch.writeByte(1)
                }

                repeat(Size / 4 - 1) {
                    ch.writeInt(0xeeeeeeeeL)
                }

                ch.flush()

                for (i in 1..shift) {
                    ch.readByte()
                }

                ch.writeInt(0x12345678)

                ch.flush()

                while (ch.availableForRead > 4) {
                    ch.readInt()
                }

                assertEquals(0x12345678, ch.readInt())
            }
        }
    }

    @Test
    fun testIntEdge2() {
        runTest {
            for (shift in 1..3) {
                for (i in 1..shift) {
                    ch.writeByte(1)
                }

                repeat(Size / 4 - 1) {
                    ch.writeInt(0xeeeeeeeeL)
                }

                ch.flush()

                for (i in 1..shift) {
                    ch.readByte()
                }

                ch.writeByte(0x12)
                ch.writeByte(0x34)
                ch.writeByte(0x56)
                ch.writeByte(0x78)

                ch.flush()

                while (ch.availableForRead > 4) {
                    ch.readInt()
                }

                assertEquals(0x12345678, ch.readInt())
            }
        }
    }


    @Test
    fun testLongB() {
        runTest {
            ch.readByteOrder = ByteOrder.BIG_ENDIAN
            ch.writeByteOrder = ByteOrder.BIG_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeLong(Long.MIN_VALUE)
            ch.flush()
            assertEquals(8, ch.availableForRead)
            assertEquals(Long.MIN_VALUE, ch.readLong())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testLongL() {
        runTest {
            ch.readByteOrder = ByteOrder.LITTLE_ENDIAN
            ch.writeByteOrder = ByteOrder.LITTLE_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeLong(Long.MIN_VALUE)
            ch.flush()
            assertEquals(8, ch.availableForRead)
            assertEquals(Long.MIN_VALUE, ch.readLong())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testLongEdge() {
        runTest {
            for (shift in 1..7) {
                for (i in 1..shift) {
                    ch.writeByte(1)
                }

                repeat(Size / 8 - 1) {
                    ch.writeLong(0x11112222eeeeeeeeL)
                }

                ch.flush()
                for (i in 1..shift) {
                    ch.readByte()
                }

                ch.writeLong(0x1234567812345678L)
                ch.flush()

                while (ch.availableForRead > 8) {
                    ch.readLong()
                }

                assertEquals(0x1234567812345678L, ch.readLong())
            }
        }
    }

    @Test
    fun testDoubleB() {
        runTest {
            ch.readByteOrder = ByteOrder.BIG_ENDIAN
            ch.writeByteOrder = ByteOrder.BIG_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeDouble(1.05)
            ch.flush()

            assertEquals(8, ch.availableForRead)
            assertEquals(1.05, ch.readDouble())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testDoubleL() {
        runTest {
            ch.readByteOrder = ByteOrder.LITTLE_ENDIAN
            ch.writeByteOrder = ByteOrder.LITTLE_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeDouble(1.05)
            ch.flush()

            assertEquals(8, ch.availableForRead)
            assertEquals(1.05, ch.readDouble())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testFloatB() {
        runTest {
            ch.readByteOrder = ByteOrder.BIG_ENDIAN
            ch.writeByteOrder = ByteOrder.BIG_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeFloat(1.05f)
            ch.flush()

            assertEquals(4, ch.availableForRead)
            assertEquals(1.05f, ch.readFloat())
            assertEquals(0, ch.availableForRead)
        }
    }

    @Test
    fun testFloatL() {
        runTest {
            ch.readByteOrder = ByteOrder.LITTLE_ENDIAN
            ch.writeByteOrder = ByteOrder.LITTLE_ENDIAN

            assertEquals(0, ch.availableForRead)
            ch.writeFloat(1.05f)
            ch.flush()

            assertEquals(4, ch.availableForRead)
            assertEquals(1.05f, ch.readFloat())
            assertEquals(0, ch.availableForRead)
        }
    }



    @Test
    fun testEndianMix() {
        val byteOrders = listOf(ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN)
        runTest {
            for (writeOrder in byteOrders) {
                ch.writeByteOrder = writeOrder

                for (readOrder in byteOrders) {
                    ch.readByteOrder = readOrder

                    assertEquals(0, ch.availableForRead)
                    ch.writeShort(0x001f)
                    ch.flush()
                    if (writeOrder == readOrder)
                        assertEquals(0x001f, ch.readShort())
                    else
                        assertEquals(0x1f00, ch.readShort())

                    assertEquals(0, ch.availableForRead)
                    ch.writeShort(0x001f)
                    ch.flush()
                    if (writeOrder == readOrder)
                        assertEquals(0x001f, ch.readShort())
                    else
                        assertEquals(0x1f00, ch.readShort())

                    assertEquals(0, ch.availableForRead)
                    ch.writeInt(0x1f)
                    ch.flush()
                    if (writeOrder == readOrder)
                        assertEquals(0x0000001f, ch.readInt())
                    else
                        assertEquals(0x1f000000, ch.readInt())

                    assertEquals(0, ch.availableForRead)
                    ch.writeInt(0x1fL)
                    ch.flush()
                    if (writeOrder == readOrder)
                        assertEquals(0x0000001f, ch.readInt())
                    else
                        assertEquals(0x1f000000, ch.readInt())

                    assertEquals(0, ch.availableForRead)
                    ch.writeLong(0x1f)
                    ch.flush()
                    if (writeOrder == readOrder)
                        assertEquals(0x1f, ch.readLong())
                    else
                        assertEquals(0x1f00000000000000L, ch.readLong())
                }
            }
        }
    }

    @Test
    fun testClose() {
        runTest {
            ch.writeByte(1)
            ch.writeByte(2)
            ch.writeByte(3)

            ch.flush()
            assertEquals(1, ch.readByte())
            ch.close()

            assertEquals(2, ch.readByte())
            assertEquals(3, ch.readByte())

            try {
                ch.readByte()
                fail()
            } catch (expected: EOFException) {
            } catch (expected: NoSuchElementException) {
            }
        }
    }

    @Test
    fun testReadAndWriteFully() {
        runTest {
            val bytes = byteArrayOf(1, 2, 3, 4, 5)
            val dst = ByteArray(5)

            ch.writeFully(bytes)
            ch.flush()
            assertEquals(5, ch.availableForRead)
            ch.readFully(dst)
            assertTrue { dst.contentEquals(bytes) }

            ch.writeFully(bytes)
            ch.flush()

            val dst2 = ByteArray(4)
            ch.readFully(dst2)

            assertEquals(1, ch.availableForRead)
            assertEquals(5, ch.readByte())

            ch.close()

            try {
                ch.readFully(dst)
                fail("")
            } catch (expected: EOFException) {
            } catch (expected: NoSuchElementException) {
            }
        }
    }

    @Test
    fun testReadAndWritePartially() {
        runTest {
            val bytes = byteArrayOf(1, 2, 3, 4, 5)

            assertEquals(5, ch.writeAvailable(bytes))
            ch.flush()
            assertEquals(5, ch.readAvailable(ByteArray(100)))

            repeat(Size / bytes.size) {
                assertNotEquals(0, ch.writeAvailable(bytes))
                ch.flush()
            }

            ch.readAvailable(ByteArray(ch.availableForRead - 1))
            assertEquals(1, ch.readAvailable(ByteArray(100)))

            ch.close()
        }
    }

    @Test
    fun testPacket() = runTest {
        val packet = buildPacket {
            writeInt(0xffee)
            writeStringUtf8("Hello")
        }

        ch.writeInt(packet.remaining)
        ch.writePacket(packet)

        ch.flush()

        val size = ch.readInt()
        val readed = ch.readPacket(size)

        assertEquals(0xffee, readed.readInt())
        assertEquals("Hello", readed.readUTF8Line())
    }

    @Test
    fun testBigPacket() = runTest {
        launch {
            val packet = buildPacket {
                writeInt(0xffee)
                writeStringUtf8(".".repeat(8192))
            }

            ch.writeInt(packet.remaining)
            ch.writePacket(packet)

            ch.flush()
        }

        val size = ch.readInt()
        val readed = ch.readPacket(size)

        assertEquals(0xffee, readed.readInt())
        assertEquals(".".repeat(8192), readed.readUTF8Line())
    }

    @Test
    fun testWriteString() = runTest {
        ch.writeStringUtf8("abc")
        ch.close()

        assertEquals("abc", ch.readUTF8Line())
    }

    @Test
    fun testWriteCharSequence() = runTest {
        ch.writeStringUtf8("abc" as CharSequence)
        ch.close()

        assertEquals("abc", ch.readUTF8Line())
    }

    @Test
    fun testWriteSuspendable() = runTest {
        launch {
            expect(2)
            val bytes = ByteArray(10)
            ch.readFully(bytes, 0, 3)
            assertEquals("1 2 3 0 0 0 0 0 0 0", bytes.joinToString(separator = " ") { it.toString() })
            expect(3)

            ch.readFully(bytes, 3, 3)
            assertEquals("1 2 3 4 5 6 0 0 0 0", bytes.joinToString(separator = " ") { it.toString() })

            expect(5)
        }

        ch.writeSuspendSession {
            expect(1)
            val b1 = request(1)!!
            b1.writeFully(byteArrayOf(1, 2, 3))
            written(3)
            flush()

            yield()

            expect(4)
            val b2 = request(1)!!
            b2.writeFully(byteArrayOf(4, 5, 6))
            written(3)
            flush()
            yield()
        }

        finish(6)
    }

    @Test
    fun testWriteSuspendableWrap() = runTest {
        var read = 0
        var written = 0

        launch {
            val bytes = ByteArray(10)

            while (true) {
                val rc = ch.readAvailable(bytes)
                if (rc == -1) break
                read += rc
            }
        }

        ch.writeSuspendSession {
            val b1 = request(1)!!
            val size1 = b1.writeRemaining
            val ba = ByteArray(size1)
            repeat(size1) {
                ba[it] = (it % 64).toByte()
            }
            written = size1
            b1.writeFully(ba)
            written(size1)
            flush()

            assertNull(request(1))

            tryAwait(3)

            val b2 = request(3)!!
            b2.writeFully(byteArrayOf(1, 2, 3))
            written(3)
            written += 3
            flush()
            yield()
        }

        ch.close()
        yield()

        assertEquals(written, read)
    }

    @Test
    fun testBigTransfer(): Unit = runTest {
        val size = 262144 + 512

        launch {
            ch.writeFully(ByteArray(size))
            ch.close()
        }

        val packet = ch.readRemaining()
        try {
            assertEquals(size.toLong(), packet.remaining)
        } finally {
            packet.release()
        }
    }

    @Test
    fun testConstruct() = runTest {
        val channel = ByteReadChannel(ByteArray(2))
        channel.readRemaining().use { rem ->
            rem.discardExact(2)
        }
    }

    private fun assertEquals(expected: Float, actual: Float) {
        if (abs(expected - actual) > 0.000001f) {
            kotlin.test.assertEquals(expected, actual)
        }
    }
}
