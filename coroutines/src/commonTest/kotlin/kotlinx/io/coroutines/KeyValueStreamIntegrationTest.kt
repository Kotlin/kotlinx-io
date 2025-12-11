/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.coroutines

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyValueStreamIntegrationTest {

    private fun parseKeyValue(line: String): Pair<String, String> {
        val parts = line.split(":", limit = 2)
        return if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            parts[0].trim() to ""
        }
    }

    @Test
    fun basicKeyValueStreaming() = runTest {
        val source = Buffer().apply {
            writeString("name:Alice\n")
            writeString("age:30\n")
            writeString("city:NewYork\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(3, entries.size)
        assertEquals("name" to "Alice", entries[0])
        assertEquals("age" to "30", entries[1])
        assertEquals("city" to "NewYork", entries[2])
    }

    @Test
    fun filteringKeyValueStream() = runTest {
        val source = Buffer().apply {
            writeString("metric:cpu\n")
            writeString("value:45.2\n")
            writeString("metric:memory\n")
            writeString("value:2048\n")
            writeString("metric:disk\n")
            writeString("value:512\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        // Filter only metric keys
        val metrics = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .filter { (key, _) -> key == "metric" }
            .toList()

        assertEquals(3, metrics.size)
        assertEquals("cpu", metrics[0].second)
        assertEquals("memory", metrics[1].second)
        assertEquals("disk", metrics[2].second)
    }

    @Test
    fun largeKeyValueStream() = runTest {
        val source = Buffer().apply {
            repeat(1000) { index ->
                writeString("record_$index:value_$index\n")
            }
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(1000, entries.size)
        assertEquals("record_0" to "value_0", entries[0])
        assertEquals("record_500" to "value_500", entries[500])
        assertEquals("record_999" to "value_999", entries[999])
    }

    @Test
    fun keyValueStreamWithEmptyLines() = runTest {
        val source = Buffer().apply {
            writeString("status:starting\n")
            writeString("\n") // Empty line
            writeString("status:running\n")
            writeString("\n") // Another empty line
            writeString("status:complete\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .filter { it.isNotBlank() } // Filter empty lines
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(3, entries.size)
        assertEquals("starting", entries[0].second)
        assertEquals("running", entries[1].second)
        assertEquals("complete", entries[2].second)
    }

    @Test
    fun valueWithColons() = runTest {
        val source = Buffer().apply {
            writeString("timestamp:2025-12-09T10:00:00Z\n")
            writeString("url:https://example.com:8080/path\n")
            writeString("message:Error: Connection failed\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(3, entries.size)
        assertEquals("timestamp" to "2025-12-09T10:00:00Z", entries[0])
        assertEquals("url" to "https://example.com:8080/path", entries[1])
        assertEquals("message" to "Error: Connection failed", entries[2])
    }

    @Test
    fun buildingMapFromStream() = runTest {
        val source = Buffer().apply {
            writeString("host:localhost\n")
            writeString("port:8080\n")
            writeString("protocol:https\n")
            writeString("timeout:30\n")
            writeString("retries:3\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val config = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()
            .toMap()

        assertEquals(5, config.size)
        assertEquals("localhost", config["host"])
        assertEquals("8080", config["port"])
        assertEquals("https", config["protocol"])
        assertEquals("30", config["timeout"])
        assertEquals("3", config["retries"])
    }

    @Test
    fun multipleSourcesAggregation() = runTest {
        fun createMetricSource(sourceId: String, count: Int): Source = Buffer().apply {
            repeat(count) { index ->
                writeString("source:$sourceId\n")
                writeString("seq:$index\n")
            }
        }

        val decoder1 = DelimitingByteStreamDecoder()
        val decoder2 = DelimitingByteStreamDecoder()

        val source1Data = createMetricSource("server1", 10)
            .asFlow(decoder1, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        val source2Data = createMetricSource("server2", 15)
            .asFlow(decoder2, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(20, source1Data.size) // 10 source + 10 seq
        assertEquals(30, source2Data.size) // 15 source + 15 seq

        val allData = source1Data + source2Data
        assertEquals(50, allData.size)
    }

    @Test
    fun streamingCounterMetrics() = runTest {
        val source = Buffer().apply {
            writeString("requests:100\n")
            writeString("errors:5\n")
            writeString("requests:150\n")
            writeString("errors:8\n")
            writeString("requests:200\n")
            writeString("errors:3\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val metrics = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        // Sum all requests
        val totalRequests = metrics
            .filter { (key, _) -> key == "requests" }
            .sumOf { (_, value) -> value.toIntOrNull() ?: 0 }

        // Sum all errors
        val totalErrors = metrics
            .filter { (key, _) -> key == "errors" }
            .sumOf { (_, value) -> value.toIntOrNull() ?: 0 }

        assertEquals(450, totalRequests)
        assertEquals(16, totalErrors)
    }

    @Test
    fun veryLongValues() = runTest {
        val longValue = "x".repeat(10000)
        val source = Buffer().apply {
            writeString("short:value\n")
            writeString("long:$longValue\n")
            writeString("another:data\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        assertEquals(3, entries.size)
        assertEquals("short" to "value", entries[0])
        assertEquals("long", entries[1].first)
        assertEquals(10000, entries[1].second.length)
        assertEquals("another" to "data", entries[2])
    }

    @Test
    fun keyValuePipelineWithTransformation() = runTest {
        val source = Buffer().apply {
            writeString("temperature:20.5\n")
            writeString("humidity:65\n")
            writeString("temperature:22.3\n")
            writeString("humidity:70\n")
            writeString("temperature:19.8\n")
            writeString("humidity:60\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        data class Measurement(val type: String, val value: Double)

        val measurements = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .map { (key, value) -> Measurement(key, value.toDoubleOrNull() ?: 0.0) }
            .toList()

        assertEquals(6, measurements.size)

        // Calculate average temperature
        val avgTemp = measurements
            .filter { it.type == "temperature" }
            .map { it.value }
            .average()

        assertTrue(avgTemp > 20.0 && avgTemp < 22.0)

        // Calculate average humidity
        val avgHumidity = measurements
            .filter { it.type == "humidity" }
            .map { it.value }
            .average()

        assertEquals(65.0, avgHumidity)
    }

    @Test
    fun propertyFileStyleParsing() = runTest {
        // Similar to Java .properties files
        val source = Buffer().apply {
            writeString("app.name:MyApplication\n")
            writeString("app.version:1.0.0\n")
            writeString("app.debug:true\n")
            writeString("db.host:localhost\n")
            writeString("db.port:5432\n")
            writeString("db.name:mydb\n")
            writeString("db.user:admin\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val properties = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()
            .toMap()

        // Verify app properties
        assertEquals("MyApplication", properties["app.name"])
        assertEquals("1.0.0", properties["app.version"])
        assertEquals("true", properties["app.debug"])

        // Verify db properties
        val dbProperties = properties.filterKeys { it.startsWith("db.") }
        assertEquals(4, dbProperties.size)
        assertEquals("localhost", dbProperties["db.host"])
        assertEquals("5432", dbProperties["db.port"])
    }

    @Test
    fun eventStreamProcessing() = runTest {
        val source = Buffer().apply {
            writeString("event:login\n")
            writeString("user:alice\n")
            writeString("event:purchase\n")
            writeString("amount:99.99\n")
            writeString("event:logout\n")
            writeString("user:alice\n")
            writeString("event:login\n")
            writeString("user:bob\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        val entries = source.asFlow(decoder, READ_BUFFER_SIZE)
            .map { bytes -> bytes.decodeToString() }
            .map { line -> parseKeyValue(line) }
            .toList()

        // Count events
        val events = entries.filter { (key, _) -> key == "event" }
        assertEquals(4, events.size)

        // Get all event types
        val eventTypes = events.map { (_, value) -> value }.toSet()
        assertEquals(setOf("login", "purchase", "logout"), eventTypes)

        // Count logins
        val loginCount = events.count { (_, value) -> value == "login" }
        assertEquals(2, loginCount)
    }
}
