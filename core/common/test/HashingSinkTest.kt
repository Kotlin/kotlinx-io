/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2016 Square, Inc.
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


import kotlin.test.Test
import kotlin.test.assertEquals

class HashingSinkTest {
  private val source = Buffer()
  private val sink = Buffer()

//  @Test fun md5() {
//    val hashingSink: HashingSink = HashingSink.md5(sink)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.MD5_abc, hashingSink.hash)
//  }
//
//  @Test fun sha1() {
//    val hashingSink = sha1(sink)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.SHA1_abc, hashingSink.hash)
//  }
//
//  @Test fun sha256() {
//    val hashingSink = sha256(sink)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.SHA256_abc, hashingSink.hash)
//  }
//
//  @Test fun sha512() {
//    val hashingSink = sha512(sink)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.SHA512_abc, hashingSink.hash)
//  }
//
//  @Test fun hmacSha1() {
//    val hashingSink = hmacSha1(sink, HashingTest.HMAC_KEY)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.HMAC_SHA1_abc, hashingSink.hash)
//  }
//
//  @Test fun hmacSha256() {
//    val hashingSink = hmacSha256(sink, HashingTest.HMAC_KEY)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.HMAC_SHA256_abc, hashingSink.hash)
//  }
//
//  @Test fun hmacSha512() {
//    val hashingSink = hmacSha512(sink, HashingTest.HMAC_KEY)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.HMAC_SHA512_abc, hashingSink.hash)
//  }
//
//  @Test fun multipleWrites() {
//    val hashingSink = sha256(sink)
//    source.writeUtf8("a")
//    hashingSink.write(source, 1L)
//    source.writeUtf8("b")
//    hashingSink.write(source, 1L)
//    source.writeUtf8("c")
//    hashingSink.write(source, 1L)
//    assertEquals(HashingTest.SHA256_abc, hashingSink.hash)
//  }
//
//  @Test fun multipleHashes() {
//    val hashingSink = sha256(sink)
//    source.writeUtf8("abc")
//    hashingSink.write(source, 3L)
//    val hash_abc = hashingSink.hash
//    assertEquals(HashingTest.SHA256_abc, hash_abc)
//    source.writeUtf8("def")
//    hashingSink.write(source, 3L)
//    assertEquals(HashingTest.SHA256_def, hashingSink.hash)
//    assertEquals(HashingTest.SHA256_abc, hash_abc)
//  }
//
//  @Test fun multipleSegments() {
//    val hashingSink = sha256(sink)
//    source.write(HashingTest.r32k)
//    hashingSink.write(source, HashingTest.r32k.size.toLong())
//    assertEquals(HashingTest.SHA256_r32k, hashingSink.hash)
//  }
//
//  @Test fun readFromPrefixOfBuffer() {
//    source.writeUtf8("z")
//    source.write(HashingTest.r32k)
//    source.skip(1)
//    source.writeUtf8("z".repeat(Segment.SIZE * 2 - 1))
//    val hashingSink = sha256(sink)
//    hashingSink.write(source, HashingTest.r32k.size.toLong())
//    assertEquals(HashingTest.SHA256_r32k, hashingSink.hash)
//  }
}
