package kotlinx.io.files

import kotlinx.io.*
import kotlin.test.*

class SmokeFileTest {

    @Test
    fun testBasicFile() {
        val path = Path("test.txt")
        path.sink().use {
            it.writeUtf8("example")
        }

        path.source().use {
            assertEquals("example", it.readUtf8Line())
        }
    }
}
