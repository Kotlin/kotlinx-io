package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*

@DangerousInternalIoApi
@Deprecated("Will be removed in future releases.", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<IoBuffer>) : BytePacketBuilderBase(pool) {

}
