/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/**
 * This benchmark is based on Okio SelectBenchmark:
 * https://github.com/square/okio/blob/master/okio/jvm/jmh/src/jmh/java/com/squareup/okio/benchmarks/SelectBenchmark.java
 */
package kotlinx.io.benchmark

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.ByteString.Companion.encodeUtf8
import kotlin.random.Random

@State(Scope.Benchmark)
open class OkioSelectBenchmark : BufferBenchmarkBase() {
    /** Representative sample field names as one might find in a JSON document.  */
    private val sampleValues: List<String> = listOf(
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

    private var options = Options.of()
    private var sampleData = ByteArray(0)

    @Setup
    open fun setup() {
        val byteStrings = sampleValues.asSequence().map { (it + "\n").encodeUtf8() }.take(optionCount).toList()
         options = Options.of(*byteStrings.toTypedArray())
        val dice = Random(0)
        val sampleDataBuffer = Buffer()
        for (i in 0 until selectCount) {
            sampleDataBuffer.write(byteStrings[dice.nextInt(optionCount)].toByteArray())
        }
        sampleData = sampleDataBuffer.readByteArray()
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