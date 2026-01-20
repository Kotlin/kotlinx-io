# UnsafeByteArrayTransformation API Redesign

## Problem Statement

The current `UnsafeByteArrayTransformation` API is complex and hard to implement correctly compared to `UnsafeByteArrayProcessor`. It requires 5 abstract methods with subtle interactions:

1. `maxOutputSize(inputSize: Int): Int` - estimate output size
2. `transformIntoByteArray(...)` - incremental transform
3. `transformToByteArray(...)` - fallback for atomic transforms
4. `finalizeIntoByteArray(...)` - incremental finalization
5. `finalizeToByteArray()` - fallback for atomic finalization

**Key pain points:**
- Duplication between `transformIntoByteArray` and `transformToByteArray`
- Unclear when fallback methods are called
- `maxOutputSize` return value (-1 vs positive) changes control flow implicitly
- Complex interaction between bounded vs streaming transformation paths

## Design Goals

1. Reduce the number of abstract methods
2. Make the contract explicit through return types
3. Eliminate duplication between incremental and fallback methods
4. Keep the API flexible enough for all use cases (compression, encryption, encoding)

## New API Design

### Overview

The redesigned API has **3 abstract methods** (down from 5) and **2 result types** with clear contracts:

```kotlin
@SubclassOptInRequired(UnsafeIoApi::class)
abstract class UnsafeByteArrayTransformation : Transformation {

    class TransformResult(val consumed: Int, val produced: Int) {
        companion object {
            fun progress(consumed: Int, produced: Int) = TransformResult(consumed, produced)
            fun needMoreInput(bytes: Int) = TransformResult(-bytes, 0)
            fun needMoreOutput(bytes: Int) = TransformResult(0, -bytes)
        }

        val isNeedMoreInput: Boolean get() = consumed < 0
        val isNeedMoreOutput: Boolean get() = produced < 0
        val requiredInputSize: Int get() = if (consumed < 0) -consumed else 0
        val requiredOutputSize: Int get() = if (produced < 0) -produced else 0
    }

    class FinalizeResult(val produced: Int) {
        companion object {
            fun output(bytes: Int) = FinalizeResult(bytes)
            fun done() = FinalizeResult(0)
            fun needBuffer(size: Int) = FinalizeResult(-size)
        }

        val isDone: Boolean get() = produced == 0
        val isNeedBuffer: Boolean get() = produced < 0
        val requiredBufferSize: Int get() = if (produced < 0) -produced else 0
    }

    abstract fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult

    abstract fun finalizeIntoByteArray(
        sink: ByteArray,
        startIndex: Int,
        endIndex: Int
    ): FinalizeResult

    abstract override fun close()
}
```

### TransformResult Contract

| `consumed` | `produced` | Meaning |
|------------|------------|---------|
| `>= 0` | `>= 0` | Normal progress: consumed N bytes, produced M bytes |
| `>= 0` | `< 0` | Need larger output buffer of size `\|produced\|` |
| `< 0` | `>= 0` | Need more input, at least `\|consumed\|` bytes |
| `< 0` | `< 0` | Need both (rare) |

**Factory methods for clarity:**
- `TransformResult.progress(consumed, produced)` - normal progress
- `TransformResult.needMoreInput(bytes)` - can't proceed without more input
- `TransformResult.needMoreOutput(bytes)` - output buffer too small

### FinalizeResult Contract

| `produced` | Meaning |
|------------|---------|
| `> 0` | Wrote N bytes, may have more output |
| `= 0` | Finalization complete |
| `< 0` | Need buffer of size `\|produced\|` |

**Factory methods for clarity:**
- `FinalizeResult.output(bytes)` - produced N bytes
- `FinalizeResult.done()` - finalization complete
- `FinalizeResult.needBuffer(size)` - need larger buffer

### Removed Methods

1. **`maxOutputSize(inputSize: Int): Int`** - Replaced by `TransformResult.needMoreOutput(size)`. The implementation signals buffer requirements through return values instead of upfront estimation.

2. **`transformToByteArray(...): ByteArray`** - Base class handles buffer growing when `isNeedMoreOutput` is true.

3. **`finalizeToByteArray(): ByteArray`** - Base class handles buffer growing when `isNeedBuffer` is true.

## Base Class Implementation

### transformTo

```kotlin
override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
    if (source.exhausted()) return 0L

    return UnsafeBufferOperations.readFromHead(source) { input, inputStart, inputEnd ->
        val available = minOf(byteCount.toInt(), inputEnd - inputStart)
        var totalConsumed = 0

        while (totalConsumed < available) {
            val result = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                transformIntoByteArray(
                    input, inputStart + totalConsumed, inputStart + available,
                    output, outputStart, outputEnd
                )
            }

            when {
                result.isNeedMoreInput -> break
                result.isNeedMoreOutput -> {
                    // Allocate requested buffer and retry
                    val temp = ByteArray(result.requiredOutputSize)
                    val retryResult = transformIntoByteArray(
                        input, inputStart + totalConsumed, inputStart + available,
                        temp, 0, temp.size
                    )
                    if (retryResult.produced > 0) {
                        sink.write(temp, 0, retryResult.produced)
                    }
                    totalConsumed += retryResult.consumed
                }
                else -> {
                    totalConsumed += result.consumed
                }
            }

            if (result.consumed == 0 && result.produced == 0) break
        }

        totalConsumed
    }.toLong()
}
```

### finalizeTo

```kotlin
override fun finalizeTo(sink: Buffer) {
    while (true) {
        val result = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
            finalizeIntoByteArray(bytes, startIndex, endIndex)
        }

        when {
            result.isDone -> break
            result.isNeedBuffer -> {
                val temp = ByteArray(result.requiredBufferSize)
                val retryResult = finalizeIntoByteArray(temp, 0, temp.size)
                if (retryResult.produced > 0) {
                    sink.write(temp, 0, retryResult.produced)
                }
            }
            result.produced == 0 -> break
        }
    }
}
```

## Behavioral Changes

### No EOF from transformTo

The `transformTo` method always returns `>= 0` (bytes consumed). There is no `-1` EOF signal.

When a transformation is "finished" (e.g., Inflater detects end-of-stream marker):
- Return `TransformResult.progress(0, 0)` for any further input
- The transformation becomes a no-op, ignoring additional input
- Higher-level code handles concatenated streams if needed

### Transformation Completion Flow

1. Caller feeds data via `transformTo` until source is exhausted
2. Caller invokes `finalizeTo` to flush remaining output
3. Caller invokes `close` to release resources

## Migration Examples

### DeflaterCompressor (Before)

```kotlin
class DeflaterCompressor(private val deflater: Deflater) : UnsafeByteArrayTransformation() {

    override fun maxOutputSize(inputSize: Int): Int = -1

    override fun transformIntoByteArray(...): TransformResult {
        if (deflater.needsInput() && inputSize > 0) {
            deflater.setInput(source, sourceStartIndex, inputSize)
        }
        val produced = deflater.deflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)
        val consumed = if (deflater.needsInput()) inputSize else 0
        return TransformResult(consumed, produced)
    }

    override fun transformToByteArray(...): ByteArray {
        // Duplicate logic with buffer growing
        deflater.setInput(source, sourceStartIndex, inputSize)
        var output = ByteArray(inputSize)
        // ... growing loop ...
        return output.copyOf(totalProduced)
    }

    override fun finalizeIntoByteArray(...): Int {
        if (!finishCalled) { deflater.finish(); finishCalled = true }
        if (deflater.finished()) return -1
        return deflater.deflate(sink, startIndex, endIndex - startIndex)
    }

    override fun finalizeToByteArray(): ByteArray {
        // Duplicate logic with buffer growing
        if (!finishCalled) { deflater.finish(); finishCalled = true }
        // ... growing loop ...
        return output.copyOf(totalProduced)
    }
}
```

### DeflaterCompressor (After)

```kotlin
class DeflaterCompressor(private val deflater: Deflater) : UnsafeByteArrayTransformation() {
    private var finishCalled = false

    override fun transformIntoByteArray(
        source: ByteArray, sourceStartIndex: Int, sourceEndIndex: Int,
        sink: ByteArray, sinkStartIndex: Int, sinkEndIndex: Int
    ): TransformResult {
        val inputSize = sourceEndIndex - sourceStartIndex

        if (deflater.needsInput() && inputSize > 0) {
            deflater.setInput(source, sourceStartIndex, inputSize)
        }

        val produced = deflater.deflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult.progress(consumed, produced)
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }

        if (deflater.finished()) return FinalizeResult.done()

        val produced = deflater.deflate(sink, startIndex, endIndex - startIndex)
        return FinalizeResult.output(produced)
    }

    override fun close() {
        deflater.end()
    }
}
```

### AES-GCM Decryption (Buffering Transformation)

```kotlin
class AesGcmDecryptor(private val cipher: Cipher) : UnsafeByteArrayTransformation() {

    override fun transformIntoByteArray(
        source: ByteArray, sourceStartIndex: Int, sourceEndIndex: Int,
        sink: ByteArray, sinkStartIndex: Int, sinkEndIndex: Int
    ): TransformResult {
        val inputSize = sourceEndIndex - sourceStartIndex
        // AES-GCM buffers all input, produces nothing until finalize
        cipher.update(source, sourceStartIndex, inputSize)
        return TransformResult.progress(consumed = inputSize, produced = 0)
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult {
        val outputSize = cipher.getOutputSize(0)
        val available = endIndex - startIndex

        if (available < outputSize) {
            return FinalizeResult.needBuffer(outputSize)
        }

        val produced = cipher.doFinal(sink, startIndex)
        return FinalizeResult.output(produced)
    }

    override fun close() {}
}
```

## Comparison with UnsafeByteArrayProcessor

| Aspect | UnsafeByteArrayProcessor | UnsafeByteArrayTransformation |
|--------|--------------------------|-------------------------------|
| Purpose | Observe/compute over data | Transform data |
| Abstract methods | 1 (`process`) + `compute` + `close` | 2 (`transformIntoByteArray`, `finalizeIntoByteArray`) + `close` |
| Result types | Generic `T` from `compute` | `TransformResult`, `FinalizeResult` |
| Complexity | Simple | Moderate (but much improved) |

## Summary of Changes

| Before | After |
|--------|-------|
| 5 abstract methods | 3 abstract methods |
| Implicit bounded/streaming distinction via `maxOutputSize` | Explicit signaling via result types |
| Duplicate code in `*IntoByteArray` and `*ToByteArray` | Single method, base class handles buffer growing |
| Magic `-1` return values | Self-documenting factory methods and properties |
| EOF signal from `transformTo` | No EOF, finished transformations return `(0, 0)` |

## Future Considerations

1. **Value classes**: Both `TransformResult` and `FinalizeResult` can become value classes (`TransformResult` over `Long`, `FinalizeResult` over `Int`) for zero-allocation on JVM.

2. **Inline factory methods**: Factory methods can be marked `inline` to avoid overhead.

3. **Documentation**: Update KDoc for all methods with examples of using factory methods and checking result properties.
