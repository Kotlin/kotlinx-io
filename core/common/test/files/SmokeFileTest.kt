package kotlinx.io.files

import kotlinx.io.*
import kotlin.test.*

class SmokeFileTest {
    private var tempFile: String? = null

    @BeforeTest
    fun setup() {
        tempFile = createTempFile()
    }

    @AfterTest
    fun cleanup() {
        deleteFile(tempFile!!)
    }

    @Test
    fun testBasicFile() {
        val path = Path(tempFile!!)
        path.sink().use {
            it.writeUtf8("example")
        }

        path.source().use {
            assertEquals("example", it.readUtf8Line())
        }
    }
}
