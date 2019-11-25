package kotlinx.io

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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
    fun testFileSize(){
        assertEquals(40000, Files.size(testFile))
    }

    @Test
    fun fileRead() {
        testFile.read(4000) {
            val i = readInt()
            assertEquals(1000, i)
        }

        testFile.read(40001) {
            assertFails {readInt()  }
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
}