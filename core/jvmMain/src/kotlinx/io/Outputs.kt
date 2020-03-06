package kotlinx.io

import kotlinx.io.internal.*
import java.io.*

/**
 * Returns an [OutputStream] that uses the current [Output] as the destination.
 * Closing the resulting [OutputStream] will close the input.
 */
public fun Output.asOutputStream(): OutputStream = OutputStreamFromOutput(this)

/**
 * Returns an [Output] that uses the current [OutputStream] as the destination.
 * Closing the resulting [Output] will close the input stream.
 */
public fun OutputStream.asOutput(): Output = OutputFromOutputStream(this)
