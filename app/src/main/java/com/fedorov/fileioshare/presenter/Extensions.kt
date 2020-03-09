package com.fedorov.fileioshare.presenter

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun InputStream.toFile(name: String, cacheDir: File): File {
    val outputFile = File(cacheDir, name)

    use { input ->
        val outputStream = FileOutputStream(outputFile)
        outputStream.use { output ->
            val buffer = ByteArray(4 * 1024) // buffer size
            while (true) {
                val byteCount = input.read(buffer)
                if (byteCount < 0) break
                output.write(buffer, 0, byteCount)
            }
            output.flush()
        }
    }

    return outputFile
}