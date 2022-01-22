package com.fedorov.fileioshare.screen.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fedorov.fileioshare.EXTRA_KEY_FILE
import com.fedorov.fileioshare.EXTRA_KEY_TYPE
import com.fedorov.fileioshare.MAX_FILE_SIZE
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.screen.model.ApplicationState
import com.fedorov.fileioshare.utils.DispatcherProvider
import com.fedorov.fileioshare.utils.Notification.showFileSizeError
import com.fedorov.fileioshare.utils.Notification.showNotificationError
import com.fedorov.fileioshare.worker.UploadingWorker
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModelFactory(
    private val applicationContext: Context,
    private val dispatcherProvider: DispatcherProvider,
) :
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(FileHandlerImpl(applicationContext), dispatcherProvider) as T
    }
}

class MainViewModel(
    private val contentResolver: FileHandler,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _applicationState = MutableSharedFlow<ApplicationState>()

    @FlowPreview
    val applicationState = _applicationState.debounce(APPLICATION_STATE_TIMEOUT_MILLIS)

    fun startUploadingFile(context: Context, uri: Uri, cacheDir: File) {
        viewModelScope.launch(dispatcherProvider.io) {
            _applicationState.emit(ApplicationState.IDLE)
            val file = contentResolver.getFileOutput(uri, cacheDir)

            file?.let { fileOutput ->
                if (fileOutput.length() < MAX_FILE_SIZE) {
                    val fileData = workDataOf(
                        EXTRA_KEY_FILE to fileOutput.name,
                        EXTRA_KEY_TYPE to contentResolver.getType(uri),
                    )

                    val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadingWorker>()
                        .setInputData(fileData)
                        .build()

                    WorkManager.getInstance(context)
                        .enqueue(uploadWorkRequest)
                } else {
                    showFileSizeError(context, fileOutput.name)
                }
                _applicationState.emit(ApplicationState.EXIT)
            } ?: withContext(dispatcherProvider.foreground) {
                showNotificationError(context)
            }
        }
    }

    companion object {
        const val APPLICATION_STATE_TIMEOUT_MILLIS: Long = 10_000
    }
}
