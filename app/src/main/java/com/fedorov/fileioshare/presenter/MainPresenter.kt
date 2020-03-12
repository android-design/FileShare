package com.fedorov.fileioshare.presenter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.fedorov.fileioshare.ACTION_INTENT_KEY
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.service.FileIOForegroundService
import com.fedorov.fileioshare.view.MainView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import java.io.File

class MainPresenter(private val context: Context, private val contentResolver: FileHandler) : MvpPresenter<MainView>() {

    private fun getFileFromUri(uri: Uri, cacheDir: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = contentResolver.getFileOutput(uri, cacheDir)

            file?.let {
                if (file.length() < 5_368_709_120) {
                    startForegroundService(it)
                } else {
                    withContext(Dispatchers.Main) {
                        // TODO Rewrite to notifications.
                        //viewState.showError("File size should be less then 5 Gb.")
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                // TODO Rewrite to notifications.
                //viewState.showError("Not success")
            }
        }
    }

    fun continueProcessingWithIntent() {
        viewState.handleIntent()
    }

    fun starting(intent: Intent, cacheDir:File){
//        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                        ?.let { uri ->
//                            val photoPickerIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
//                            photoPickerIntent.data = uri

                                getFileFromUri(uri, cacheDir)

                                viewState.finishActivity()


                        }
    }

    private fun startForegroundService(file: File) {
        val serviceIntent = Intent(context, FileIOForegroundService::class.java)
        serviceIntent.putExtra(ACTION_INTENT_KEY, file)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}