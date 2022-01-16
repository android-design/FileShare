package com.fedorov.fileioshare.screen.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fedorov.fileioshare.EXTRA_KEY_FILE
import com.fedorov.fileioshare.EXTRA_KEY_TYPE
import com.fedorov.fileioshare.MAX_FILE_SIZE
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.screen.model.ApplicationState
import com.fedorov.fileioshare.service.FileUploaderForegroundService
import com.fedorov.fileioshare.utils.DispatcherProvider
import com.fedorov.fileioshare.utils.NotificationImpl.showFileSizeError
import com.fedorov.fileioshare.utils.NotificationImpl.showNotificationError
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModelFactory(private val applicationContext: Context) :
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(FileHandlerImpl(applicationContext), DispatcherProvider()) as T
    }
}

class MainViewModel(
    private val contentResolver: FileHandler,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _applicationState = MutableStateFlow(ApplicationState.IDLE)
    @FlowPreview
    val applicationState = _applicationState.debounce(TIMEOUT_MILLIS)

    fun startUploadingFile(context: Context, uri: Uri, cacheDir: File) {
        viewModelScope.launch(dispatcherProvider.io) {
            val file = contentResolver.getFileOutput(uri, cacheDir)

            file?.let { fileOutput ->
                if (fileOutput.length() < MAX_FILE_SIZE) {
                    val type = contentResolver.getType(uri)
                    startForegroundService(context, fileOutput, type)
                } else {
                    showFileSizeError(context, fileOutput.name)
                }
                _applicationState.value = ApplicationState.EXIT
            } ?: withContext(dispatcherProvider.foreground) {
                showNotificationError(context)
            }
        }
    }

    private fun startForegroundService(context: Context, file: File, type: String?) {
        val serviceIntent = Intent(context, FileUploaderForegroundService::class.java)
        serviceIntent.putExtra(EXTRA_KEY_FILE, file)
        serviceIntent.putExtra(EXTRA_KEY_TYPE, type)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val TIMEOUT_MILLIS: Long = 10_000
    }
}
