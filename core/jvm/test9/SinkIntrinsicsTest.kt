/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

class Java9BufferIntArrayWriteTest : AbstractIntArrayWriteTest(SinkFactory.BUFFER)
class Java9SinkIntArrayWriteTest : AbstractIntArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferShortArrayWriteTest : AbstractShortArrayWriteTest(SinkFactory.BUFFER)
class Java9SinkShortArrayWriteTest : AbstractShortArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferLongArrayWriteTest : AbstractLongArrayWriteTest(SinkFactory.BUFFER)
class Java9SinkLongArrayWriteTest : AbstractLongArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferFloatArrayWriteTest : AbstractFloatArrayWriteTest(SinkFactory.BUFFER)
class Java9SinkFloatArrayWriteTest : AbstractFloatArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferDoubleArrayWriteTest : AbstractDoubleArrayWriteTest(SinkFactory.BUFFER)
class Java9SinkDoubleArrayWriteTest : AbstractDoubleArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferIntArrayLeWriteTest : AbstractIntArrayLeWriteTest(SinkFactory.BUFFER)
class Java9SinkIntArrayLeWriteTest : AbstractIntArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferShortArrayLeWriteTest : AbstractShortArrayLeWriteTest(SinkFactory.BUFFER)
class Java9SinkShortArrayLeWriteTest : AbstractShortArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferLongArrayLeWriteTest : AbstractLongArrayLeWriteTest(SinkFactory.BUFFER)
class Java9SinkLongArrayLeWriteTest : AbstractLongArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferFloatArrayLeWriteTest : AbstractFloatArrayLeWriteTest(SinkFactory.BUFFER)
class Java9SinkFloatArrayLeWriteTest : AbstractFloatArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class Java9BufferDoubleArrayLeWriteTest : AbstractDoubleArrayLeWriteTest(SinkFactory.BUFFER)
class Java9SinkDoubleArrayLeWriteTest : AbstractDoubleArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)
