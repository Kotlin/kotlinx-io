package kotlinx.io.files

import kotlinx.io.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.*
import java.nio.file.Path as NioPath

public actual class Path internal constructor(internal val nioPath: NioPath)

public actual fun Path(path: String): Path = Path(Paths.get(path))

public actual fun Path.source(): Source = FileInputStream(nioPath.toFile()).source().buffer()

public actual fun Path.sink(): Sink = FileOutputStream(nioPath.toFile()).sink().buffer()
