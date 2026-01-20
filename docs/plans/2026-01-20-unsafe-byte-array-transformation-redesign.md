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

    class TransformResult private constructor(
        private val _consumed: Int,
        private val _produced: Int
    ) {
        // For ok results - actual bytes processed
        val consumed: Int get() = _consumed
        val produced: Int get() = if (_produced >= 0) _produced else 0

        // For requirement results - buffer size needed (0 if not required)
        val outputRequired: Int get() = if (_produced < 0) -_produced else 0

        companion object {
            fun ok(consumed: Int, produced: Int) = TransformResult(consumed, produced)  // consumed, produced >= 0
            fun done() = TransformResult(0, 0)  // transformation complete
            fun outputRequired(size: Int) = TransformResult(0, -size)  // size > 0
        }
    }

    class FinalizeResult private constructor(private val _produced: Int) {
        // For ok results
        val produced: Int get() = if (_produced >= 0) _produced else 0

        // For requirement results
        val outputRequired: Int get() = if (_produced < 0) -_produced else 0

        // Completion check
        val isDone: Boolean get() = _produced == 0

        companion object {
            fun ok(produced: Int) = FinalizeResult(produced)  // produced > 0
            fun done() = FinalizeResult(0)  // finalization complete
            fun outputRequired(size: Int) = FinalizeResult(-size)  // size > 0
        }
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

| Internal state | Meaning | Access pattern |
|----------------|---------|----------------|
| `_consumed >= 0, _produced >= 0` | Normal progress | `consumed`, `produced` |
| `_produced < 0` | Need larger output buffer | `outputRequired > 0` |

**Factory methods:**
- `TransformResult.ok(consumed, produced)` - normal progress (consumed, produced >= 0)
- `TransformResult.done()` - transformation complete (equivalent to `ok(0, 0)`)
- `TransformResult.outputRequired(size)` - output buffer too small (size > 0)

**Access properties:**
- `consumed` / `produced` - actual bytes processed (0 if requirement result)
- `outputRequired` - buffer size needed (0 if ok result)

### FinalizeResult Contract

| Internal state | Meaning | Access pattern |
|----------------|---------|----------------|
| `_produced > 0` | Wrote bytes, may have more | `produced > 0` |
| `_produced == 0` | Finalization complete | `isDone == true` |
| `_produced < 0` | Need larger buffer | `outputRequired > 0` |

**Factory methods:**
- `FinalizeResult.ok(produced)` - produced N bytes
- `FinalizeResult.done()` - finalization complete
- `FinalizeResult.outputRequired(size)` - need larger buffer

**Access properties:**
- `produced` - actual bytes produced (0 if done or requirement result)
- `outputRequired` - buffer size needed (0 if ok/done result)
- `isDone` - true when finalization is complete

### Removed Methods

1. **`maxOutputSize(inputSize: Int): Int`** - Replaced by `TransformResult.outputRequired(size)`. The implementation signals buffer requirements through return values instead of upfront estimation.

2. **`transformToByteArray(...): ByteArray`** - Base class handles buffer growing when `outputRequired > 0`.

3. **`finalizeToByteArray(): ByteArray`** - Base class handles buffer growing when `outputRequired > 0`.

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
                result.outputRequired > 0 -> {
                    // Allocate requested buffer and retry
                    val temp = ByteArray(result.outputRequired)
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
            result.outputRequired > 0 -> {
                val temp = ByteArray(result.outputRequired)
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
- Return `TransformResult.done()` for any further input
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

        return TransformResult.ok(consumed, produced)
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }

        if (deflater.finished()) return FinalizeResult.done()

        val produced = deflater.deflate(sink, startIndex, endIndex - startIndex)
        return FinalizeResult.ok(produced)
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
        return TransformResult.ok(consumed = inputSize, produced = 0)
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult {
        val outputSize = cipher.getOutputSize(0)
        val available = endIndex - startIndex

        if (available < outputSize) {
            return FinalizeResult.outputRequired(outputSize)
        }

        val produced = cipher.doFinal(sink, startIndex)
        return FinalizeResult.ok(produced)
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
| Implicit bounded/streaming distinction via `maxOutputSize` | Explicit signaling via `outputRequired` |
| Duplicate code in `*IntoByteArray` and `*ToByteArray` | Single method, base class handles buffer growing |
| Magic `-1` return values | Self-documenting `ok()`, `done()`, `outputRequired()` |
| EOF signal from `transformTo` | No EOF, finished transformations return `ok(0, 0)` |

## API Quick Reference

### TransformResult

```kotlin
// Factory methods
TransformResult.ok(consumed, produced)      // normal progress
TransformResult.done()                      // transformation complete
TransformResult.outputRequired(size)        // need larger output buffer

// Access properties
result.consumed        // bytes consumed
result.produced        // bytes produced (0 if requirement result)
result.outputRequired  // output size needed (0 if ok result)
```

### FinalizeResult

```kotlin
// Factory methods
FinalizeResult.ok(produced)          // produced bytes
FinalizeResult.done()                // finalization complete
FinalizeResult.outputRequired(size)  // need larger buffer

// Access properties
result.produced        // bytes produced (0 if done/requirement)
result.outputRequired  // output size needed (0 if ok/done)
result.isDone          // true when finalization complete
```

## Future Considerations

1. **Value classes**: Both `TransformResult` and `FinalizeResult` can become value classes (`TransformResult` over `Long`, `FinalizeResult` over `Int`) for zero-allocation on JVM.

2. **Inline factory methods**: Factory methods can be marked `inline` to avoid overhead.

3. **Documentation**: Update KDoc for all methods with examples of using factory methods and checking result properties.
