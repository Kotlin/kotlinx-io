package kotlinx.io.core

import kotlinx.io.pool.*

actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<IoBuffer>) : BytePacketBuilderBase(pool) {

}
