package com.fedorov.fileioshare.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

class FileHandlerImpl(private val context: Context) : FileHandler {

    override fun fileNameFromUri(uri: Uri): String {
        var fileName = ""
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        }
            .onSuccess {
                fileName = it.orEmpty()
            }
            .onFailure {
                fileName = uri.path?.substringAfterLast("/") ?: ""
            }
        return fileName
    }

    override fun getFileInputStream(uri: Uri): InputStream {
        return context.contentResolver.openInputStream(uri) ?: error("No input stream")
    }

    override fun getType(uri: Uri): String = context.contentResolver.getType(uri).orEmpty()

    override fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)
            cursor.moveToFirst()
            return cursor.getLong(nameIndex)
        }
        return 0
    }
}
