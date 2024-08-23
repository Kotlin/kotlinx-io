# kotlinx-io benchmarks

The module consists of benchmarks aimed to track performance of kotlinx-io implementation.

Currently, the suite includes benchmarks on:
- the core Buffer API usage: read/write primitive types, arrays, UTF8 strings;
- basic `peek` usage;
- segment pooling performance.

The suite doesn't include benchmarks for more complex APIs inherited from Okio as these APIs are subject to change.
Such benchmarks will be added later along with corresponding changes in the library.

### Quickstart

For JVM:
```
./gradlew :kotlinx-io-benchmarks:jvmJar

java -jar benchmarks/build/benchmarks/jvm/jars/kotlinx-io-benchmarks-jvm-jmh-0.6.0-SNAPSHOT-JMH.jar  ReadStringBenchmark -f 1 -wi 5 -i 5 -tu us -w 1 -r 1
```