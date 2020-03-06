package kotlinx.io

import kotlinx.io.buffer.*

public actual object Console {

    public actual val input: Input = SystemIn
    public actual val output: Output = SystemOut
    public actual val error: Output = SystemErr
}

private object SystemIn : Input() {

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        /*
         * Implementation is optimized for the **default** System.in that
         * is buffered and provides precise information about available bytes.
         */
        val sin = System.`in`
        var current = startIndex
        if (sin.available() <= 0) {
            val byte = sin.read().toByte()
            if (byte < 0) {
                return current - startIndex
            }
            buffer[current++] = byte
        }

        repeat(sin.available()) {
            val byte = sin.read().toByte()
            if (byte < 0) {
                return current
            }
            buffer[current++] = byte
        }
        return current - startIndex
    }

    override fun closeSource() {
        throw IllegalStateException("Console.input cannot be closed")
    }
}


private object SystemOut : Output() {

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean {
        val out = System.out
        for (i in startIndex until endIndex) {
            out.write(source[i].toInt())
        }
        out.flush()

        return true
    }

    override fun closeSource() {
        throw IllegalStateException("Console.output cannot be closed")
    }
}


private object SystemErr : Output() {

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean {
        val out = System.err
        for (i in startIndex until endIndex) {
            out.write(source[i].toInt())
        }
        out.flush()

        return true
    }

    override fun closeSource() {
        throw IllegalStateException("Console.error cannot be closed")
    }
}
