package kotlinx.io.net.selector

private const val SELECTOR_BUFFER_SIZE = 8192

internal abstract class CommonSelectorManager : SelectorManager {
    private val messageBox = SafeSelectablesHeap(SELECTOR_BUFFER_SIZE)

    /**
     * Poll for new events from underlying selector and write selected instances to [buffer]
     * @return number of selected entries or 0 if there are no events selected
     */
    protected abstract fun poll(buffer: Array<Selectable?>): Int

    /**
     * Similar to [poll] except that it blocks and waits for events if there are no events available
     * @return number of selected entries or 0 if selection has been cancelled
     */
    protected abstract fun waitFor(buffer: Array<Selectable?>, timeoutMillis: Long): Int

    /**
     * Configure underlying selector to listen to particular events of s
     */
    protected abstract fun interest(s: Selectable)

    /**
     * Selection loop coroutine
     */
    protected suspend fun loop() {
        val buffer = arrayOfNulls<Selectable?>(SELECTOR_BUFFER_SIZE)
        val registered = SelectablesHeap(SELECTOR_BUFFER_SIZE)
        var doWait = false

        while (true) {
            if (registered.isEmpty) {
                // TODO wait for select invocation
                continue
            }

            val count = if (doWait) waitFor(buffer, 500) else poll(buffer)

            repeat(count) { idx ->
                val s = buffer[idx]!!
                if (processSelected(s)) {
                    registered.remove(s)
                }
            }

            buffer.zero(count)

            val hasNewRegistered = processMessageBox(registered)

            val waited = doWait
            doWait = count == 0 && !hasNewRegistered

            if (waited && doWait) {
                yield() // TODO ???
            }
        }
    }

    private fun processMessageBox(registered: SelectablesHeap): Boolean {
        val mb = messageBox
        var registeredCount = 0

        while (true) {
            val s = mb.poll() ?: break

            interest(s)
            registered.add(s)
            registeredCount++
        }

        return registeredCount > 0
    }

    private fun processSelected(s: Selectable): Boolean {
        // TODO process
        return s.interestOps == 0
    }

    override fun notifyClosed(s: Selectable) {
    }

    override suspend fun select(selectable: Selectable, interest: SelectInterest) {
    }

    override fun close() {
    }
}


internal expect class SafeSelectablesHeap(size: Int) {
    fun add(element: Selectable): Boolean
    fun poll(): Selectable?
}

private class SelectablesHeap(size: Int) {
    private var elements = arrayOfNulls<Selectable?>(size)
    private var size = 0

    val isEmpty: Boolean get() = size == 0

    fun add(element: Selectable): Boolean {
        if (element.localHeapIndex != -1) return false

        if (size == elements.size) {
            grow()
        }

        val index = size
        size = index + 1

        elements[index] = element
        element.localHeapIndex = index

        return true
    }

    fun remove(element: Selectable): Boolean {
        val index = element.localHeapIndex
        if (index == -1) return false

        val size = size
        val elements = elements

        if (index >= size) throw IllegalArgumentException("Most likely the selectable is not from this heap")

        if (size == 1) {
            elements[0] = null
            this.size = 0
        } else {
            val lastIndex = size - 1
            elements[index] = elements[lastIndex]
            elements[lastIndex] = null
            this.size = lastIndex
        }

        element.localHeapIndex = -1

        return true
    }

    private fun grow() {
        elements = elements.copyOf(elements.size * 2)
    }
}

internal expect fun <T : Any> Array<T?>.zero(count: Int)
internal expect suspend fun yield()