package kotlinx.io

import kotlinx.io.buffer.*
import java.lang.IllegalStateException

public actual object Console {

    public actual val input: Input = SystemIn
    public actual val output: Output = SystemOut
    public actual val error: Output = SystemErr
}

private object SystemIn : Input() {

    override fun fill(buffer: Buffer): Int {
        /*
         * Implementation is optimized for the **default** System.in that
         * is buffered and provides precise information about available bytes.
         */
        val sin = System.`in`
        var filled = 0
        if (sin.available() <= 0) {
            val byte = sin.read().toByte()
            if (byte < 0) {
                return filled
            }
            buffer[filled++] = byte
        }

        repeat(sin.available()) {
            val byte = sin.read().toByte()
            if (byte < 0) {
                return filled
            }
            buffer[filled++] = byte
        }
        return filled
    }

    override fun closeSource() {
        throw IllegalStateException("Console.input cannot be closed")
    }
}


private object SystemOut : Output() {

    override fun flush(source: Buffer, length: Int) {
        val out = System.out
        for (i in 0 until length) {
            out.write(source[i].toInt())
        }
        out.flush()
    }

    override fun closeSource() {
        throw IllegalStateException("Console.output cannot be closed")
    }
}


private object SystemErr : Output() {

    override fun flush(source: Buffer, length: Int) {
        val out = System.err
        for (i in 0 until length) {
            out.write(source[i].toInt())
        }
        out.flush()
    }

    override fun closeSource() {
        throw IllegalStateException("Console.error cannot be closed")
    }
}

