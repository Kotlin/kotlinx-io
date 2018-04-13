package kotlinx.io.net.selector

/**
 * A selectable entity that could be processed by [SelectorManager].
 * One selectable could be associated only with one particular [SelectorManager].
 */
expect class Selectable {
    internal var id: Int

    internal var localHeapIndex: Int
    internal var safeHeapIndex: Int

    internal var readyOps: Int
    internal var interestOps: Int

    fun close()
}

/**
 * Selector manager is a service that manages selectors and selection threads. Uses some platform-specific way
 * to select for events. For example it could be epoll, kqueues or JVM's `java.nio.channels.Selector`
 */
interface SelectorManager {
    /**
     * Notifies the selector manager that a selectable instance has been closed.
     * Could release related underlying resources or do nothing.
     * Does nothing for closed selector manager.
     * This is usually called by selectable primitives implementation and usually shouldn't be used by end users
     */
    fun notifyClosed(s: Selectable)

    /**
     * Suspends until [interest] is selected for [selectable]
     * May cause manager to allocate and run selector instance if not yet created.
     *
     * Only one selection is allowed per [interest] per [selectable] but you can
     * select for different interests for the same selectable simultaneously.
     * In other words you can select for read and write at the same time but should never
     * try to read twice for the same selectable
     *
     * This is usually called by selectable primitives implementation and usually shouldn't be used by end users
     *
     * @throws IllegalStateException cancellation exception if selector is closed or malfunctional for some reason.
     */
    suspend fun select(selectable: Selectable, interest: SelectInterest)

    /**
     * Closes selector manager. All underlying resources will be released, all coroutine suspensions on [select] will
     * be cancelled. All further [close] invocations will be ignored and all [select] invocations will throw
     * cancellation exception. Actual implementation could also attach some platform specific exception. For example,
     * on JVM it could be `java.nio.channels.ClosedSelectorException`.
     */
    fun close()

    companion object {
        val DefaultSelectorManager = DefaultSelectorManagerImpl
    }
}

internal expect val DefaultSelectorManagerImpl: SelectorManager

expect enum class SelectInterest {
    READ,
    WRITE,
    ACCEPT,
    CONNECT;

    internal val flag: Int

    companion object {
        val AllInterests: Array<SelectInterest>
        internal val flags: IntArray
    }
}

private val byFlags = Array(SelectInterest.values().maxBy { it.flag }!!.flag + 1) { flag ->
    SelectInterest.values().firstOrNull { it.flag == flag }
}

internal fun SelectInterest.Companion.byFlag(flag: Int): SelectInterest =
    (if (flag in 0 until byFlags.size) byFlags[flag] else null)
            ?: throw IllegalArgumentException("SelectInterest not found for flag $flag")

