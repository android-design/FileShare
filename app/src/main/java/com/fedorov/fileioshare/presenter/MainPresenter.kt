package com.fedorov.fileioshare.presenter

import android.net.Uri
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.view.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import moxy.presenterScope
import java.io.File

class MainPresenter(private val contentResolver: FileHandler) : MvpPresenter<MainView>() {

    fun getFileFromUri(uri: Uri, cacheDir: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = contentResolver.getFileOutput(uri, cacheDir)

            file?.let {
                if (file.length() < 5_368_709_120) {
                    viewState.startForegroundService(it)
                } else {
                    withContext(Dispatchers.Main) {
                        viewState.showError("File size should be less then 5 Gb.")
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                viewState.showError("Not success")
            }
            viewState.finishActivity()
        }
    }

    fun continueProcessingWithIntent() {
        viewState.handleIntent()
    }
}