package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*

@DangerousInternalIoApi
@Deprecated("Will be removed in future releases.")
@Suppress("DEPRECATION")
actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<ChunkBuffer>) : BytePacketBuilderBase(pool) {

}
