/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Marks declarations that should be used carefully and in some cases, may cause data corruption or loss.
 *
 * Consider using other APIs instead when possible.
 * Otherwise, make sure to read documentation describing a delicate API.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate API and its use requires care. " +
            "Make sure you fully read and understand documentation of the declaration that is marked as a delicate API."
)
public annotation class DelicateIoApi

/**
 * Marks declarations that are **internal** in IO API.
 * These declarations may change or be removed without notice, and not intended for public use.
 * Incorrect of declarations marked as internal may cause data corruption or loss.
 *
 * Consider using other APIs instead when possible.
 * Otherwise, make sure to read documentation describing
 * an internal API.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal API and its use requires care. " +
            "It is subject to change or removal and is not intended for use outside the library." +
            "Make sure you fully read and understand documentation of the declaration that " +
            "is marked as an internal API."
)
public annotation class InternalIoApi

/**
 * Marks an experimental API available only in snapshot builds.
 * There are no guarantees regarding the stability of such an API,
 * and it is a subject of change or removal without notice.
 */
@SnapshotApi
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(message = "This is a snapshot API, it may not be available in regular library release.")
public annotation class SnapshotApi
