package kotlinx.io.streams

import kotlinx.io.pool.*

internal val ByteArrayPool = object : DefaultPool<ByteArray>(128) {
    final override fun produceInstance(): ByteArray {
        return ByteArray(4096)
    }

    final override fun validateInstance(instance: ByteArray) {
        require(instance.size == 4096) { "Unable to recycle buffer of wrong size: ${instance.size} != 4096" }
        super.validateInstance(instance)
    }
}
