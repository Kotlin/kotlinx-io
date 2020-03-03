package kotlinx.io.text

import kotlinx.io.*

/**
 * Reads the line in UTF-8 encoding from the input until the next line break or until the input is exhausted.
 * A line break is either `"\n"` or `"\r\n"` and is not included in the result.
 * @throws EOFException if the input is already exhausted
 */
public fun Input.readUtf8Line(): String {
    checkExhausted()
    return buildString {
        readUtf8LineTo(this)
    }
}

/**
 * Reads the whole input as a list of UTF-8 encoded lines separated by a line break
 * and closes the input when it is exhausted.
 * A line break is either `"\n"` or `"\r\n"` and is not included in resulting strings.
 *
 * @throws EOFException if the input is already exhausted
 */
public fun Input.readUtf8Lines(): List<String> {
    checkExhausted()
    val list = ArrayList<String>()
    use {
        while (!exhausted()) {
            list += readUtf8Line()
        }
    }
    return list
}

/**
 * Iterates through each line of the input, calls [action] for each line read
 * and closes the input when it is exhausted.
 *
 * @throws EOFException if the input is already exhausted
 */
public inline fun Input.forEachUtf8Line(action: (String) -> Unit) {
    use {
        while (!exhausted()) {
            action(readUtf8Line())
        }
    }
}

private fun Input.checkExhausted() {
    if (exhausted()) throw EOFException("Unexpected EOF while reading UTF-8 line")
}
