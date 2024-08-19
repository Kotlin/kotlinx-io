/*
 * Copyright 2017-$today.yer JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.test.Test

class CommonIOStreamsTest {
    @Test
    public fun writeToStdout() {
        val out = stdoutSink().buffered()
        out.writeString("Hello World!")
        out.flush()
    }
}
