@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.io.external

import kotlin.js.JsAny
import kotlin.js.js

/** Interop type for JavaScript `Uint8Array`. */
public external class Uint8Array : JsAny {
    public constructor(buffer: ArrayBuffer)

    public val length: Double
    public val buffer: ArrayBuffer
}

public operator fun Uint8Array.get(index: Double): Byte =
    get(this, index)

public operator fun Uint8Array.set(index: Double, value: Byte): Unit =
    set(this, index, value)

private fun get(array: Uint8Array, index: Double): Byte =
    js("array[index]")

private fun set(array: Uint8Array, index: Double, value: Byte): Unit =
    js("array[index] = value")
