/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
/*
 * Copyright (C) 2019 Square, Inc.
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
package kotlinx.io.select

import kotlinx.io.Buffer
import kotlinx.io.Segment
import kotlin.math.min

public fun Buffer.select(options: Options): Int {
    val index = selectPrefix(options)
    if (index == -1) return -1

    // If the prefix match actually matched a full byte string, consume it and return it.
    val selectedSize = options.byteStrings[index].size
    skip(selectedSize.toLong())
    return index
}

internal fun Buffer.selectPrefix(options: Options, selectTruncated: Boolean = false): Int {
    val head = head ?: return if (selectTruncated) -2 else -1

    var s: Segment? = head
    var data = head.data
    var pos = head.pos
    var limit = head.limit

    val trie = options.trie
    var triePos = 0

    var prefixIndex = -1

    navigateTrie@
    while (true) {
        val scanOrSelect = trie[triePos++]

        val possiblePrefixIndex = trie[triePos++]
        if (possiblePrefixIndex != -1) {
            prefixIndex = possiblePrefixIndex
        }

        val nextStep: Int

        if (s == null) {
            break@navigateTrie
        } else if (scanOrSelect < 0) {
            // Scan: take multiple bytes from the buffer and the trie, looking for any mismatch.
            val scanByteCount = -1 * scanOrSelect
            val trieLimit = triePos + scanByteCount
            while (true) {
                val byte = data[pos++].toInt() and 0xff
                if (byte != trie[triePos++]) return prefixIndex // Fail 'cause we found a mismatch.
                val scanComplete = (triePos == trieLimit)

                // Advance to the next buffer segment if this one is exhausted.
                if (pos == limit) {
                    s = s!!.next!!
                    pos = s.pos
                    data = s.data
                    limit = s.limit
                    if (s === head) {
                        if (!scanComplete) break@navigateTrie // We were exhausted before the scan completed.
                        s = null // We were exhausted at the end of the scan.
                    }
                }

                if (scanComplete) {
                    nextStep = trie[triePos]
                    break
                }
            }
        } else {
            // Select: take one byte from the buffer and find a match in the trie.
            val selectChoiceCount = scanOrSelect
            val byte = data[pos++].toInt() and 0xff
            val selectLimit = triePos + selectChoiceCount
            while (true) {
                if (triePos == selectLimit) return prefixIndex // Fail 'cause we didn't find a match.

                if (byte == trie[triePos]) {
                    nextStep = trie[triePos + selectChoiceCount]
                    break
                }

                triePos++
            }

            // Advance to the next buffer segment if this one is exhausted.
            if (pos == limit) {
                s = s.next!!
                pos = s.pos
                data = s.data
                limit = s.limit
                if (s === head) {
                    s = null // No more segments! The next trie node will be our last.
                }
            }
        }

        if (nextStep >= 0) return nextStep // Found a matching option.
        triePos = -nextStep // Found another node to continue the search.
    }

    // We break out of the loop above when we've exhausted the buffer without exhausting the trie.
    if (selectTruncated) return -2 // The buffer is a prefix of at least one option.
    return prefixIndex // Return any matches we encountered while searching for a deeper match.
}

public fun Buffer.selectWithIter(options: Options): Int {
    val index = selectPrefixWithIter(options)
    if (index == -1) return -1

    // If the prefix match actually matched a full byte string, consume it and return it.
    val selectedSize = options.byteStrings[index].size
    skip(selectedSize.toLong())
    return index
}

internal fun Buffer.selectPrefixWithIter(options: Options, selectTruncated: Boolean = false): Int {
    val iter = segments()
    if (!iter.hasNext()) {
        return if (selectTruncated) -2 else -1
    }

    val trie = options.trie
    var triePos = 0

    var prefixIndex = -1

    var seg = iter.next()
    var pos = 0
    var limit = seg.size
    var exhausted = false

    navigateTrie@
    while (true) {
        val scanOrSelect = trie[triePos++]

        val possiblePrefixIndex = trie[triePos++]
        if (possiblePrefixIndex != -1) {
            prefixIndex = possiblePrefixIndex
        }

        val nextStep: Int

        if (exhausted) {
            break@navigateTrie
        } else if (scanOrSelect < 0) {
            // Scan: take multiple bytes from the buffer and the trie, looking for any mismatch.
            val scanByteCount = -1 * scanOrSelect
            val trieLimit = triePos + scanByteCount
            while (true) {
                val iters = minOf(trieLimit - triePos, limit - pos)
                for (i in 0 until  iters) {
                    val byte = seg[pos++].toInt() and 0xff
                    val trieVal = trie[triePos++]
                    if (byte != trieVal) return prefixIndex
                }
                val scanComplete = (triePos == trieLimit)
                // Advance to the next buffer segment if this one is exhausted.
                if (pos == limit) {
                    if (!iter.hasNext()) {
                        if (!scanComplete) break@navigateTrie // We were exhausted before the scan completed.
                        exhausted = true // We were exhausted at the end of the scan.
                    } else {
                        seg = iter.next()
                        pos = 0
                        limit = seg.size
                    }
                }

                if (scanComplete) {
                    nextStep = trie[triePos]
                    break
                }
            }
        } else {
            // Select: take one byte from the buffer and find a match in the trie.
            val selectChoiceCount = scanOrSelect
            val byte = seg[pos++].toInt() and 0xff
            val selectLimit = triePos + selectChoiceCount

            var index = -1
            for (idx in maxOf(0, triePos) until min(selectLimit, trie.size)) {
                if (trie[idx] == byte) {
                    index = idx
                    break
                }
            }
            if (index == -1) return prefixIndex
            triePos = index
            nextStep = trie[triePos + selectChoiceCount]

            // Advance to the next buffer segment if this one is exhausted.
            if (pos == limit) {
                if (iter.hasNext()) {
                    seg = iter.next()
                    pos = 0
                    limit = seg.size
                } else {
                    exhausted = true // No more segments! The next trie node will be our last.
                }
            }
        }

        if (nextStep >= 0) return nextStep // Found a matching option.
        triePos = -nextStep // Found another node to continue the search.
    }

    // We break out of the loop above when we've exhausted the buffer without exhausting the trie.
    if (selectTruncated) return -2 // The buffer is a prefix of at least one option.
    return prefixIndex // Return any matches we encountered while searching for a deeper match.
}
