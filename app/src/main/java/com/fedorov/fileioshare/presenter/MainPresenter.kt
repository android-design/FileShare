package com.fedorov.fileioshare.presenter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fedorov.fileioshare.EXTRA_KEY_FILE
import com.fedorov.fileioshare.EXTRA_KEY_TYPE
import com.fedorov.fileioshare.MAX_FILE_SIZE
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.service.FileUploaderForegroundService
import com.fedorov.fileioshare.utils.notificationId
import com.fedorov.fileioshare.view.MainView
import com.fedorov.fileioshare.view.NotificationImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moxy.MvpPresenter
import java.io.File

class MainPresenter(private val context: Context, private val contentResolver: FileHandler) :
    MvpPresenter<MainView>() {

    private fun getFileFromUri(uri: Uri, cacheDir: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = contentResolver.getFileOutput(uri, cacheDir)

            file?.let { fileOutput ->
                if (fileOutput.length() < MAX_FILE_SIZE) {
                    val type = contentResolver.getType(uri)
                    startForegroundService(fileOutput, type)
                } else {
                    showFileSizeError(fileOutput.name)
                }
            } ?: withContext(Dispatchers.Main) {
                showNotificationError()
            }
        }
    }

    private fun showFileSizeError(fileName: String) {
        val builder = NotificationImpl.notificationGrouped(
            context = context,
            contentTitle = "Error. $fileName, " + context.getString(R.string.error_file_size)
        )
        NotificationManagerCompat.from(context).apply {
            notify(notificationId(), builder.build())
        }
    }

    private fun showNotificationError() {
        val builder = NotificationImpl.notificationGrouped(
            context = context,
            contentTitle = "Error. " + context.getString(R.string.error_can_not_read_file)
        )
        NotificationManagerCompat.from(context).apply {
            notify(notificationId(), builder.build())
        }
    }

    fun continueProcessingWithIntent() {
        viewState.handleIntent()
    }

    fun starting(intent: Intent, cacheDir: File) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
            ?.let { uri ->
                getFileFromUri(uri, cacheDir)

                viewState.finishActivity()
            }
    }

    private fun startForegroundService(file: File, type: String?) {
        val serviceIntent = Intent(context, FileUploaderForegroundService::class.java)
        serviceIntent.putExtra(EXTRA_KEY_FILE, file)
        serviceIntent.putExtra(EXTRA_KEY_TYPE, type)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}