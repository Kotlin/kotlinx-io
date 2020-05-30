package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BinaryTest {

    @Test fun `creating array binary performs defensive copy`() {
        // arrange
        val originalData = "Hello".encodeToByteArray()
        val first = originalData.asBinary(defensiveCopy = true)

        // act
        val second = originalData.apply { set(0, 'B'.toByte()) }.asBinary()

        // assert
        assertNotEquals(first, second)
    }

    @Test fun `creating a binary array without defensive copy is not immutable`() {
        // arrange
        val originalData = "Hello".encodeToByteArray()
        val first = originalData.asBinary(defensiveCopy = false)

        // act
        val second = originalData.apply { set(0, 'B'.toByte()) }.asBinary()

        // assert
        assertEquals(first, second)
    }

    @Test fun `array binary gets the proper byte of data`() {
        // arrange
        val data = "Hello".encodeToByteArray().asBinary()

        // act
        val result = data[4]

        // assert
        assertEquals('o'.toByte(), result)
    }

    @Test fun `array binary contains byte`() {
        // arrange
        val data = "Hello".encodeToByteArray().asBinary()

        // act
        val result = 'o'.toByte() in data

        // assert
        assertTrue(result)
    }

    @Test fun `array binary is proper size`() {
        // arrange
        val data = "Hello".encodeToByteArray().asBinary()

        // act
        val result = data.size

        // assert
        assertEquals(5, result)
    }

    @Test fun `array binary is equal to another array binary`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()
        val second = "Hello".encodeToByteArray().asBinary()

        // assert
        assertEquals(first, second)
    }

    @Test fun `binary slice with identical content is equal to array binary`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()

        // act
        val second = first.slice()

        // assert
        assertEquals(first, second)
    }

    @Test fun `binary slice must not have negative start index`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()

        // assert
        assertFailsWith(IllegalArgumentException::class) {
            // act
            first.slice(startIndex = -1)
        }
    }

    @Test fun `binary slice endIndex must be greater than startIndex`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()

        // assert
        assertFailsWith(IllegalArgumentException::class) {
            // act
            first.slice(startIndex = 1, endIndex = 0)
        }
    }

    @Test fun `binary slice endIndex cannot exceed last index of source`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()

        // assert
        assertFailsWith(IllegalArgumentException::class) {
            // act
            first.slice(endIndex = 5)
        }
    }

    @Test fun `binary slice get operator maps to proper index`() {
        // arrange
        val first = "Hello".encodeToByteArray().asBinary()

        // act
        val second = first.slice(1, 3)

        // assert
        assertEquals("ell".encodeToByteArray().asBinary(), second)
    }

    @Test fun `binary slice contains byte`() {
        // arrange
        val data = "Hello".encodeToByteArray().asBinary().slice().slice(1, 3)

        // act
        val result = 'e'.toByte() in data

        // assert
        assertTrue(result, message = "Binary slice 'ell' should contain byte 'e'")
    }

    @Test fun `binary slice does not contain byte`() {
        // arrange
        val data = "Hello".encodeToByteArray().asBinary().slice().slice(1, 3)

        // act
        val result = 'H'.toByte() in data

        // assert
        assertFalse(result, message = "Binary slice 'ell' should not contain byte 'H'")
    }

    @Test fun `chunking binary produces binaries of equal size`() {
        // arrange
        val data = "Foo Bar Baz Bax".encodeToByteArray().asBinary()

        // act
        val result = data.chunked(size = 3)

        // assert
        assertEquals(5, result.size)
        result.forEach { assertEquals(3, it.size) }
    }

    @Test fun `chunking aligned binary produces binaries with correct data`() {
        // arrange
        val data = "Foo Bar Baz Bax"
        val expected = data.chunked(size = 3).map { it.encodeToByteArray().asBinary() }

        // act
        val result = data.encodeToByteArray().asBinary().chunked(size = 3)

        // assert
        assertEquals(expected, result)
    }

    @Test fun `chunking unaligned binary produces smaller component in last result index`() {
        // arrange
        val data = "Foo Bar Baz Bax".encodeToByteArray().asBinary()

        // act
        val result = data.chunked(size = 4)

        // assert
        assertEquals(3, result.last().size)
    }

    @Test fun `chunking unaligned binary produces correct data`() {
        // arrange
        val data = "Foo Bar Baz Bax"
        val expected = data.chunked(size = 4).map { it.encodeToByteArray().asBinary() }

        // act
        val result = data.encodeToByteArray().asBinary().chunked(size = 4)

        // assert
        assertEquals(expected, result)
    }

    @Test fun `getting byte array from array binary has correct data`() {
        // arrange
        val arrayBinary = "Hello".encodeToByteArray().asBinary()

        // act
        val result = arrayBinary.toByteArray()

        // assert
        assertTrue("Hello".encodeToByteArray().contentEquals(result))
    }

    @Test fun `getting byte array from slice binary has correct data`() {
        // arrange
        val arrayBinary = "Hello".encodeToByteArray().asBinary().slice()

        // act
        val result = arrayBinary.toByteArray()

        // assert
        assertTrue("Hello".encodeToByteArray().contentEquals(result))
    }

    @Test fun `encoding binary value as hex prodices string of correct size`() {
        // arrange
        val binary = "Hello".encodeToByteArray().asBinary()

        // act
        val result = binary.hex

        // assert
        assertEquals(10, result.length)
    }

    @Test fun `encoding binary value as hex produces the right string`() {
        // arrange
        val binary = "Hello".encodeToByteArray().asBinary()

        // act
        val result = binary.hex

        // assert
        assertEquals("48656c6c6f", result)
    }

    @Test fun `decoding hex string to binary fails when not byte aligned`() {
        // arrange
        val invalid = "484"

        // assert
        assertFailsWith(IllegalArgumentException::class) {
            // act
            Binary.fromHexString(invalid)
        }
    }

    @Test fun `decoding hex string with invalid characters fails`() {
        // arrange
        val invalid = "0k"

        // assert
        assertFailsWith(IllegalArgumentException::class) {
            // act
            Binary.fromHexString(invalid)
        }
    }

    @Test fun `decoding valid hex string produces binary with correct value`() {
        // arrange
        val hexString = "cafebabe"

        // act
        val binary = Binary.fromHexString(hexString)

        // assert
        assertEquals(byteArrayOf(-54, -2, -70, -66).asBinary(), binary)
    }

    @Test fun `decoding valid uppercase hex string produces binary with correct value`() {
        // arrange
        val hexString = "CAFEBABE"

        // act
        val binary = Binary.fromHexString(hexString)

        // assert
        assertEquals(byteArrayOf(-54, -2, -70, -66).asBinary(), binary)
    }

    @Test fun `encoding binary as base64 produces correct value`() {

    }
}