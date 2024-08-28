/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring.samples

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.io.bytestring.*
import platform.Foundation.*
import kotlin.test.*

class ByteStringSamplesApple {
    @OptIn(UnsafeNumber::class, ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
    @Test
    fun nsDataConversion() {
        val originalByteString: ByteString = "Compress me, please!".encodeToByteString()

        val compressedNSData: NSData = originalByteString.toNSData().compressedDataUsingAlgorithm(
            algorithm = NSDataCompressionAlgorithmZlib,
            error = null
        )!!

        val compressedByteString: ByteString = compressedNSData.toByteString()
        assertEquals("73cecf2d284a2d2e56c84dd55128c8494d2c4e550400", compressedByteString.toHexString())
        // If there's no zlib-flate on your path, you can test it using:
        // zlib.decompress(binascii.unhexlify("73cecf2d284a2d2e56c84dd55128c8494d2c4e550400"), -15)
    }
}
