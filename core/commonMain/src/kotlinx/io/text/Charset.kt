package kotlinx.io.text

import kotlinx.io.*

expect abstract class Charset {
    abstract fun newEncoder(): CharsetEncoder
    abstract fun newDecoder(): CharsetDecoder

    companion object {
        fun forName(name: String): Charset
    }
}

expect val Charset.name: String
