/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefCounteringCopyTrackerTest {
    @Test
    fun stateTransition() {
        val tracker = RefCountingCopyTracker()
        assertFalse(tracker.shared)

        assertFalse(tracker.removeCopyIfShared())
        assertFalse(tracker.shared)

        tracker.addCopy()
        assertTrue(tracker.shared)
        assertTrue(tracker.removeCopyIfShared())
        assertFalse(tracker.shared)

        tracker.addCopy()
        assertTrue(tracker.shared)
        tracker.addCopy()
        assertTrue(tracker.shared)
        assertTrue(tracker.removeCopyIfShared())
        assertTrue(tracker.shared)
        assertTrue(tracker.removeCopyIfShared())
        assertFalse(tracker.shared)
    }
}
