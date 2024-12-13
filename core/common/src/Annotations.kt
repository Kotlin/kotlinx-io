/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Marks API that may cause data corruption or loss or behave unpredictable when used with invalid argument values.
 *
 * Consider using other APIs instead when possible.
 * Otherwise, make sure to read documentation describing an unsafe API.
 */
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is an unsafe API and its use requires care. " +
            "Make sure you fully understand documentation of the declaration marked as UnsafeIoApi"
)
public annotation class UnsafeIoApi

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Suppress("ClassName")
public annotation class _Discardable
