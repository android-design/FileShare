package com.fedorov.fileioshare.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.fedorov.fileioshare.presenter.toFile
import java.io.File


class FileHandlerImpl(private val context: Context) : FileHandler {

    private fun fileNameFromUri(uri: Uri): String {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                return cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            // TODO: Timber time ;)
            Log.d("Test", "")
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
            // TODO: Timber time.
            Log.d("Errore", e.localizedMessage!!)
        }
        return null
    }
}