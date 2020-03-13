package com.fedorov.fileioshare.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.fedorov.fileioshare.presenter.toFile
import timber.log.Timber
import java.io.File


class FileHandlerImpl(private val context: Context) : FileHandler {

    private fun fileNameFromUri(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }

        return uri.path?.substringAfterLast("/") ?: ""
    }

    override fun getFileOutput(uri: Uri, cacheDir: File): File? {

        try {
            val fileName = fileNameFromUri(uri)

            if (fileName.isEmpty()) {
                throw IllegalStateException("Filename not found")
            }


            context.contentResolver.openInputStream(uri)
                ?.let { stream ->
                    return stream.toFile(fileName, cacheDir)
                }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    override fun getType(uri: Uri): String? = context.contentResolver.getType(uri)
}