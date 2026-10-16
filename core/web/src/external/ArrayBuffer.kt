@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.io.external

import kotlin.js.JsAny

/** Interop type for JavaScript `ArrayBuffer`. */
public external class ArrayBuffer : JsAny {
    public constructor(length: Double)
    public constructor(length: Double, options: Options)

    public fun resize(length: Double)

    public interface Options : JsAny {
        public var maxByteLength: Double
    }
}
