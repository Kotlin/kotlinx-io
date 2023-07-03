# kotlinx-io benchmarks

The module consists of benchmarks aimed to track performance of kotlinx-io implementation.

Currently, the suite includes benchmarks on:
- the core Buffer API usage: read/write primitive types, arrays, UTF8 strings;
- basic `peek` usage;
- segment pooling performance.

The suite doesn't include benchmarks for more complex APIs inherited from Okio as these APIs are subject to change.
Such benchmarks will be added later along with corresponding changes in the library.