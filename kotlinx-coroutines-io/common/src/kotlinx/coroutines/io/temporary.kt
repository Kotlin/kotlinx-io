package kotlinx.coroutines.io

import kotlinx.coroutines.CancellationException

@Deprecated("Use coroutines cancellation exception instead",
    replaceWith = ReplaceWith("CancellationException", "kotlinx.coroutines.CancellationException"))
typealias CancellationException = CancellationException
