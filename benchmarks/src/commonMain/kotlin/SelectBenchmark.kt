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
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readByteString
import kotlinx.io.select.Options
import kotlinx.io.select.select
import kotlinx.io.write
import kotlin.random.Random

@State(Scope.Benchmark)
open class SelectBenchmark {
    /** Representative sample field names as one might find in a JSON document.  */
    var sampleValues: List<String> = mutableListOf(
        "id", "name", "description", "type", "sku_ids",
        "offers", "start_time", "end_time", "expires", "start_of_availability", "duration",
        "allow_recording", "thumbnail_id", "thumbnail_formats", "is_episode", "is_live", "channel_id",
        "genre_list", "provider_networks", "year", "video_flags", "is_repeat", "series_id",
        "series_name", "series_description", "original_air_date", "letter_box", "category",
        "child_protection_rating", "parental_control_minimum_age", "images", "episode_id",
        "season_number", "episode_number", "directors_list", "scriptwriters_list", "actors_list",
        "drm_rights", "is_location_chk_reqd", "is_catchup_enabled", "catchup_duration",
        "is_timeshift_enabled", "timeshift_duration", "is_startover_enabled", "is_recording_enabled",
        "suspension_time", "shared_ref_id", "linked_channel_number", "audio_lang", "subcategory",
        "metadata_root_id", "ref_id", "ref_type", "display_position", "thumbnail_format_list",
        "network", "external_url", "offer_type", "em_format", "em_artist_name", "assets",
        "media_class", "media_id", "channel_number"
    )

    @Param("4", "8", "16", "32", "64")
    var optionCount = 0

    @Param("2048")
    var selectCount = 0

    private var buffer: Buffer = Buffer()
    private var options: Options = Options.of()
    private var sampleData: ByteString = ByteString()

    @Setup
    fun setup() {
        val byteStrings = Array(optionCount) { ByteString() }
        for (i in 0 until optionCount) {
            byteStrings[i] = (sampleValues[i] + "\"").encodeToByteString()
        }
        options = Options.of(*byteStrings)
        val dice = Random(0)
        val sampleDataBuffer = Buffer()
        for (i in 0 until selectCount) {
            sampleDataBuffer.write(byteStrings[dice.nextInt(optionCount)])
        }
        sampleData = sampleDataBuffer.readByteString()
    }

    @Benchmark
    fun select(blackhole: Blackhole) {
        buffer.write(sampleData)
        for (i in 0 until selectCount) {
            blackhole.consume(buffer.select(options))
        }
        if (!buffer.exhausted()) throw AssertionError()
    }
}
