package kotlinx.io.internal.jvm

fun negativeShiftError(delta: Int): Nothing =
    throw IllegalStateException("Wrong buffer position change: negative shift $delta")

fun limitChangeError(): Nothing = throw IllegalStateException("Limit change is now allowed")
fun wrongBufferPositionChangeError(delta: Int, size: Int): Nothing =
    throw IllegalStateException("Wrong buffer position change: $delta. " +
            "Position should be moved forward only by at most size bytes (size = $size)")
