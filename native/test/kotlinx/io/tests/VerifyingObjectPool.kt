package kotlinx.io.tests

import kotlinx.io.pool.*

@SymbolName("Kotlin_Any_hashCode")
private external fun identityHashCodeImpl(any: Any?): Int

@Suppress("NOTHING_TO_INLINE")
internal inline actual fun identityHashCode(instance: Any): Int = identityHashCodeImpl(instance)

actual class VerifyingObjectPool<T : Any> actual constructor(delegate: ObjectPool<T>) : VerifyingPoolBase<T>(delegate) {
    override val allocated = HashSet<IdentityWrapper<T>>()
}
