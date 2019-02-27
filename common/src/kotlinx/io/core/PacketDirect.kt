package kotlinx.io.core

@PublishedApi
internal inline fun AbstractInput.read(n: Int = 1, block: (Buffer) -> Unit) {
    val buffer = prepareRead(n) ?: prematureEndOfStream(n)
    val positionBefore = buffer.readPosition
    try {
        block(buffer)
    } finally {
        val positionAfter = buffer.readPosition
        if (positionAfter < positionBefore) {
            throw IllegalStateException("Buffer's position shouldn't be rewinded")
        }
        if (positionAfter == buffer.writePosition) {
            ensureNext(buffer)
        } else {
            headPosition = positionAfter
        }
    }
}
