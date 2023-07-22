/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(UnsafeNumber::class)

package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import platform.Foundation.*
import platform.posix.memcpy
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal fun ByteArray.toNSData() = if (isNotEmpty()) {
    usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.convert())
    }
} else {
    NSData.data()
}

fun NSData.toByteArray() = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        memcpy(refTo(0), bytes, length)
    }
}

fun startRunLoop(name: String = "run-loop"): NSRunLoop {
    val created = Mutex(true)
    lateinit var runLoop: NSRunLoop
    val thread = NSThread {
        runLoop = NSRunLoop.currentRunLoop
        runLoop.addPort(NSMachPort.port(), NSDefaultRunLoopMode)
        created.unlock()
        runLoop.run()
    }
    thread.name = name
    thread.start()
    runBlocking {
        created.lockWithTimeout()
    }
    return runLoop
}

suspend fun Mutex.lockWithTimeout(timeout: Duration = 5.seconds) {
    class MutexSource : Throwable()
    val source = MutexSource()
    try {
        withTimeout(timeout) { lock() }
    } catch (e: TimeoutCancellationException) {
        fail("Mutex never unlocked", source)
    }
}

fun NSStreamEvent.asString(): String {
    return when (this) {
        NSStreamEventNone -> "NSStreamEventNone"
        NSStreamEventOpenCompleted -> "NSStreamEventOpenCompleted"
        NSStreamEventHasBytesAvailable -> "NSStreamEventHasBytesAvailable"
        NSStreamEventHasSpaceAvailable -> "NSStreamEventHasSpaceAvailable"
        NSStreamEventErrorOccurred -> "NSStreamEventErrorOccurred"
        NSStreamEventEndEncountered -> "NSStreamEventEndEncountered"
        else -> "Unknown event $this"
    }
}
