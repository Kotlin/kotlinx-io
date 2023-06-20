/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

/*
public fun ByteString.encodeToString(charset: Charset = Charsets.UTF_8): String = getByteArray().toString(charset)
public fun ByteString.Companion.fromString(string: String, charset: Charset = Charsets.UTF_8): ByteString =
    ByteString(string.toByteArray(charset))
 */

/*
public fun ByteString.writeTo(outputStream: OutputStream) {
    writeCopyTo(object : ByteString.Reader {
        override fun read(data: ByteArray, beginIndex: Int, endIndex: Int) {
            outputStream.write(data, beginIndex, endIndex - beginIndex)
        }
    })
}

@OptIn(UnsafeApi::class)
public fun ByteString.writeTo(outputStream: ByteArrayOutputStream) {
    writeUnsafeTo(object : ByteString.Reader {
        override fun read(data: ByteArray, beginIndex: Int, endIndex: Int) {
            outputStream.write(data, beginIndex, endIndex - beginIndex)
        }
    })
}

@OptIn(UnsafeApi::class)
public fun ByteString.md5(): ByteString {
    val md = MessageDigest.getInstance("MD5")
    writeUnsafeTo(object : ByteString.Reader {
        override fun read(data: ByteArray, beginIndex: Int, endIndex: Int) {
            md.update(data, beginIndex, endIndex - beginIndex)
        }
    })
    md.digest()
    return ByteString(md.digest())
}

@OptIn(UnsafeApi::class)
public fun ByteString.toString(charset: Charset = Charsets.UTF_8): String {
    var string: String? = null

    writeUnsafeTo(object : ByteString.Reader {
        override fun read(data: ByteArray, beginIndex: Int, endIndex: Int) {
            string = String(data, beginIndex, endIndex - beginIndex, charset)
        }
    })

    return string!!
}
 */