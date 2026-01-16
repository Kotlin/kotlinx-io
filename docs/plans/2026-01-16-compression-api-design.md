# Compression API Design

## Overview

This document describes the redesigned compression API for kotlinx-io. The design goals are:

1. **Flexibility for algorithm-specific options** - Different algorithms need different parameters
2. **API consistency** - Unified approach for built-in and custom algorithms
3. **Extensibility** - Third-party libraries can implement additional algorithms (Zstd, Brotli, LZ4, etc.)
4. **Layered design** - Core transformation primitives that compression builds upon

## Core Artifact (`kotlinx-io-core`)

### Transform Interface

A general-purpose streaming transformation that converts input bytes to output bytes. Designed for compression/decompression but reusable for other transforms (encryption, encoding, etc.).

```kotlin
interface Transform : AutoCloseable {
    /**
     * Transform data from source to sink.
     * May not consume all input if internal buffer is full.
     */
    fun transform(source: Buffer, sink: Buffer)

    /**
     * Signal end of input and flush remaining output.
     * May need to be called multiple times until all output is flushed.
     */
    fun finish(sink: Buffer)

    /**
     * True when transform has completed and will produce no more output.
     */
    val isFinished: Boolean

    /**
     * Release resources associated with this transform.
     */
    override fun close()
}
```

### Extension Functions

```kotlin
/**
 * Returns a RawSink that transforms data written to it.
 */
fun RawSink.transform(transform: Transform): RawSink

/**
 * Returns a RawSource that transforms data read from it.
 */
fun RawSource.transform(transform: Transform): RawSource
```

### Internal Implementations

```kotlin
internal class TransformingSink(
    private val downstream: RawSink,
    private val transform: Transform
) : RawSink

internal class TransformingSource(
    private val upstream: RawSource,
    private val transform: Transform
) : RawSource
```

## Compression Artifact (`kotlinx-io-compression`)

### Compression/Decompression Interfaces

```kotlin
/**
 * Provides a compression transform.
 */
interface Compression {
    fun compressor(): Transform
}

/**
 * Provides a decompression transform.
 */
interface Decompression {
    fun decompressor(): Transform
}
```

### Built-in Algorithms

```kotlin
/**
 * DEFLATE compression (RFC 1951).
 *
 * @param level compression level 0-9 (0=none, 9=max). Default is 6.
 */
class Deflate(val level: Int = 6) : Compression, Decompression {
    override fun compressor(): Transform
    override fun decompressor(): Transform

    companion object {
        /** Creates compression-only config. */
        fun compression(level: Int = 6): Compression

        /** Creates decompression-only config. */
        fun decompression(): Decompression
    }
}

/**
 * GZIP compression (RFC 1952).
 *
 * @param level compression level 0-9 (0=none, 9=max). Default is 6.
 */
class GZip(val level: Int = 6) : Compression, Decompression {
    override fun compressor(): Transform
    override fun decompressor(): Transform

    companion object {
        fun compression(level: Int = 6): Compression
        fun decompression(): Decompression
    }
}
```

### Extension Functions

```kotlin
// Generic compression/decompression
fun RawSink.compressed(compression: Compression): RawSink =
    transform(compression.compressor())

fun RawSource.decompressed(decompression: Decompression): RawSource =
    transform(decompression.decompressor())

// Convenience functions for built-in algorithms
fun RawSink.deflate(level: Int = 6): RawSink = compressed(Deflate(level))
fun RawSource.inflate(): RawSource = decompressed(Deflate.decompression())
fun RawSink.gzip(level: Int = 6): RawSink = compressed(GZip(level))
fun RawSource.ungzip(): RawSource = decompressed(GZip.decompression())
```

## Third-Party Implementation

Third-party libraries can implement custom compression algorithms:

```kotlin
// Example: Zstd implementation in a separate library

class Zstd(val level: Int = 3) : Compression, Decompression {
    override fun compressor(): Transform = ZstdCompressorTransform(level)
    override fun decompressor(): Transform = ZstdDecompressorTransform()

    companion object {
        fun compression(level: Int = 3): Compression = Zstd(level)
        fun decompression(): Decompression = Zstd()
    }
}

internal class ZstdCompressorTransform(private val level: Int) : Transform {
    override fun transform(source: Buffer, sink: Buffer) { /* ... */ }
    override fun finish(sink: Buffer) { /* ... */ }
    override val isFinished: Boolean get() = /* ... */
    override fun close() { /* ... */ }
}

internal class ZstdDecompressorTransform : Transform {
    override fun transform(source: Buffer, sink: Buffer) { /* ... */ }
    override fun finish(sink: Buffer) { /* ... */ }
    override val isFinished: Boolean get() = /* ... */
    override fun close() { /* ... */ }
}
```

Usage:
```kotlin
sink.compressed(Zstd(level = 5))
source.decompressed(Zstd.decompression())
```

## Error Handling

Compression/decompression errors throw `IOException`.

## Usage Examples

```kotlin
// Simple compression with convenience functions
val compressed = Buffer()
compressed.gzip().buffered().use { sink ->
    sink.writeString("Hello, World!")
}

// Decompression
val text = compressed.ungzip().buffered().readString()

// Using explicit algorithm configuration
sink.compressed(Deflate(level = 9))
source.decompressed(GZip.decompression())

// Third-party algorithms
sink.compressed(Zstd(level = 5))
source.decompressed(Brotli())
```

## Design Decisions

1. **Separate `Compression`/`Decompression` interfaces** - Type-safe; can't accidentally use compression config for decompression
2. **Factory methods (`Deflate.compression()`, `Deflate.decompression()`)** - Allow future expansion with algorithm-specific parameters without breaking changes
3. **`Transform` in core** - General-purpose building block reusable for other transformations
4. **Algorithm-specific levels** - No shared `CompressionLevel` constants; each algorithm defines its own valid range
5. **Convenience functions delegate to generic API** - `gzip()` calls `compressed(GZip(...))`, ensuring consistency
