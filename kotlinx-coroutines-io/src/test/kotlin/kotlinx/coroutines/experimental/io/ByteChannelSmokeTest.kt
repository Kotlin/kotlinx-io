package kotlinx.coroutines.experimental.io

import kotlin.test.*

class ByteChannelSmokeTest {
    @Test
    fun simpleSmokeTest() {
        val bc = ByteChannel()
        bc.close()
    }
}