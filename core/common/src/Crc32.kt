/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

/**
 * Creates a [Processor] that computes the CRC-32 checksum of the processed data.
 *
 * CRC-32 is a widely used checksum algorithm that produces a 32-bit hash value.
 * This implementation uses the IEEE 802.3 polynomial (0xEDB88320).
 *
 * Example usage:
 * ```kotlin
 * val checksum = source.compute(crc32())
 * ```
 *
 * The processor accumulates data across multiple [Processor.process] calls.
 * Multiple [Processor.compute] calls return the same checksum value for the accumulated data.
 */
internal expect fun crc32(): Processor<Long>
