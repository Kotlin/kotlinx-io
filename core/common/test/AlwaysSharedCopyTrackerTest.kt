/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertTrue

class AlwaysSharedCopyTrackerTest {
    @Test
    fun stateTransition() {
        val tracker = AlwaysSharedCopyTracker
        assertTrue(tracker.shared)

        assertTrue(tracker.removeCopy())
        assertTrue(tracker.shared)

        tracker.addCopy()
        assertTrue(tracker.shared)

        assertTrue(tracker.removeCopy())
        assertTrue(tracker.shared)
    }
}
