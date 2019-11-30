package kotlinx.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

@ExperimentalIoApi
internal class FileBinaryTest {
    lateinit var testFile: Path

    @BeforeTest
    fun creatTestFile() {
        testFile = Files.createTempFile("kotlinx-io", ".bin")
        testFile.write {
            repeat(10000) {
                writeInt(it)
            }
        }
    }

    @Test
    fun testFileSize() {
        assertEquals(40000, Files.size(testFile))
    }

    @Test
    fun fileRead() {
        testFile.read(4000) {
            val i = readInt()
            assertEquals(1000, i)
        }

        testFile.read(40001) {
            assertFails { readInt() }
        }
    }

    @Test
    fun multiBinaryRead() {
        val binary = testFile.asBinary()

        binary.read(400, 4) {
            assertEquals(100, readInt())
            assertFails { readInt() }
        }

        binary.read {
            skipBytes(400)
            assertEquals(100, readInt())
            skipBytes(3600)
            assertEquals(1001, readInt())
        }
    }

    @Test
    fun testEof(){
        testFile.read {
            skipBytes(40000)
            assertTrue { eof() }
        }
    }

    @Test
    fun testMixedContent() {
        val file = Files.createTempFile("kotlinx-io-string", ".bin")
        val binary = buildBytes {
            writeInt(8)
            writeDouble(22.2)
        }
        file.write {
            writeUTF8String("Header\n")
            repeat(10) {
                writeInt(it)
            }
            writeBinary(binary)
            writeUTF8String("Footer")
        }
        println(file.toUri())
        file.read {
            assertEquals("Header", readUTF8Line())
            val ints = IntArray(10) { readInt() }
            assertEquals(5, ints[5])
            assertEquals(8, readInt())
            assertEquals(22.2, readDouble())
            assertEquals("Footer", readUTF8String())
        }
    }
}