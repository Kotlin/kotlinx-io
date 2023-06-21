/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.internal.commonAsUtf8ToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteStringExtensionsTest {
    private val bronzeHorseman = "На берегу пустынных волн"

    @Test
    fun utf8() {
        val byteString = ByteString.fromUtf8(bronzeHorseman)
        assertEquals(byteString.toByteArray().toList(), bronzeHorseman.commonAsUtf8ToByteArray().toList())
        assertEquals(byteString, ByteString(*bronzeHorseman.commonAsUtf8ToByteArray()))
        assertEquals(
            byteString,
            ByteString(
                "d09dd0b020d0b1d0b5d180d0b5d0b3d18320d0bfd183d181d182d18bd0bdd0bdd18bd18520d0b2d0bed0bbd0bd".decodeHex()
            )
        )
        assertEquals(byteString.toUtf8(), bronzeHorseman)
    }
}