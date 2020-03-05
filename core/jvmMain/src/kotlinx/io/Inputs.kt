package kotlinx.io

import kotlinx.io.internal.*
import java.io.*

/**
 * Returns an [InputStream] that uses the current [Input] as an underlying source of data.
 * Closing the resulting [InputStream] will close the input.
 */
public fun Input.asInputStream(): InputStream = InputStreamFromInput(this)

/**
 * Returns an [Input] that uses the current [InputStream] as an underlying source of data.
 * Closing the resulting [Input] will close the input stream.
 */
public fun InputStream.asInput(): Input = InputFromInputStream(this)
