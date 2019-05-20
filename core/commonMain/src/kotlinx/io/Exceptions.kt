package kotlinx.io

class EndOfFileException(message: String = "End of file") : IOException(message)

abstract class IOException(message: String) : Exception(message) 
