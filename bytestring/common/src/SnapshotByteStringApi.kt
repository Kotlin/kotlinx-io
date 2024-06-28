/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

/**
 * Marks an experimental API available only in snapshot builds.
 * There are no guarantees regarding the stability of such an API,
 * and it is a subject of change or removal without notice.
 */
@SnapshotByteStringApi
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(message = "This is a snapshot API, it may not be available in regular library release.")
public annotation class SnapshotByteStringApi
