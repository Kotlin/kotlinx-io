/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.test.Test


class T {
    @Test
    fun test() {
        val md = MessageDigest.getInstance("MD5")
        var bb = ByteBuffer.allocate(1024)
        bb.put(ByteArray(1024))
        bb = bb.asReadOnlyBuffer()
        bb.position(0)
        md.update(bb)
    }
}