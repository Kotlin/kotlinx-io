/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

/**
 * A collection of unused segments, necessary to avoid GC churn and zero-fill.
 * This pool is a thread-safe static singleton.
 */
internal expect object SegmentPool {
    val MAX_SIZE: Int

    /**
     * For testing only. Returns a snapshot of the number of bytes currently in the pool. If the pool
     * is segmented such as by thread, this returns the byte count accessible to the calling thread.
     */
    val byteCount: Int

    /** Return a segment for the caller's use. */
    fun take(): Segment

    /** Recycle a segment that the caller no longer needs. */
    fun recycle(segment: Segment)

    /**
     * Allocates a new copy tracker that'll be associated with a segment from this pool.
     * For performance reasons, there's no tracker attached to a segment initially.
     * Instead, it's allocated lazily on the first sharing attempt.
     */
    fun tracker(): SegmentCopyTracker
}
