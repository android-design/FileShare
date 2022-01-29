package com.fedorov.fileioshare.data

import android.net.Uri
import java.io.InputStream

interface FileHandler {
    fun getType(uri: Uri): String
    fun getFileSize(uri: Uri): Long
    fun fileNameFromUri(uri: Uri): String
    fun getFileInputStream(uri: Uri): InputStream
}
