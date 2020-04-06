package kotlinx.io.text

public expect abstract class Charset {
    public abstract fun newEncoder(): CharsetEncoder
    public abstract fun newDecoder(): CharsetDecoder

    public companion object {
        public fun forName(name: String): Charset
    }
}

public expect val Charset.name: String
