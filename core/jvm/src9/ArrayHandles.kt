/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import java.lang.invoke.MethodHandles
import java.nio.ByteOrder

internal object ArrayHandles {
    val shortHandle = MethodHandles.byteArrayViewVarHandle(ShortArray::class.java, ByteOrder.BIG_ENDIAN)
    val shortLeHandle = MethodHandles.byteArrayViewVarHandle(ShortArray::class.java, ByteOrder.LITTLE_ENDIAN)

    val intHandle = MethodHandles.byteArrayViewVarHandle(IntArray::class.java, ByteOrder.BIG_ENDIAN)
    val intLeHandle = MethodHandles.byteArrayViewVarHandle(IntArray::class.java, ByteOrder.LITTLE_ENDIAN)

    val longHandle = MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.BIG_ENDIAN)
    val longLeHandle = MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.LITTLE_ENDIAN)

    val floatHandle = MethodHandles.byteArrayViewVarHandle(FloatArray::class.java, ByteOrder.BIG_ENDIAN)
    val floatLeHandle = MethodHandles.byteArrayViewVarHandle(FloatArray::class.java, ByteOrder.LITTLE_ENDIAN)

    val doubleHandle = MethodHandles.byteArrayViewVarHandle(DoubleArray::class.java, ByteOrder.BIG_ENDIAN)
    val doubleLeHandle = MethodHandles.byteArrayViewVarHandle(DoubleArray::class.java, ByteOrder.LITTLE_ENDIAN)
}
