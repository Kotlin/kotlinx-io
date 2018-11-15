package kotlinx.io.core

import kotlinx.io.core.internal.*

inline fun <I : Input, R> I.use(block: (I) -> R): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

inline fun <O : Output, R> O.use(block: (O) -> R): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

//class TestInput : Input {
//}
