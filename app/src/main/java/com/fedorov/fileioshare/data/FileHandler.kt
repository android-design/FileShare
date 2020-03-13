package com.fedorov.fileioshare.data

import android.net.Uri
import java.io.File

interface FileHandler {
    fun getFileOutput(uri: Uri, cacheDir: File): File?
    fun getType(uri:Uri):String?
}