package com.fedorov.fileioshare.presentation.screen.send

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.model.ApplicationState
import com.fedorov.fileioshare.service.FileUploaderForegroundService
import com.fedorov.fileioshare.utils.DispatcherProvider
import com.fedorov.fileioshare.utils.NotificationUtils.showFileSizeError
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class SendViewModelFactory(
    private val applicationContext: Context,
    private val dispatcherProvider: DispatcherProvider,
) :
    ViewModelProvider.NewInstanceFactory() {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SendViewModel(FileHandlerImpl(applicationContext), dispatcherProvider) as T
    }
}

class SendViewModel(
    private val contentResolver: FileHandler,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _applicationState = MutableSharedFlow<ApplicationState>()

    @FlowPreview
    val applicationState = _applicationState.debounce(APPLICATION_STATE_TIMEOUT_MILLIS)

    fun startUploadingFile(context: Context, uri: Uri) {
        viewModelScope.launch(dispatcherProvider.io) {
            _applicationState.emit(ApplicationState.IDLE)
            if (contentResolver.getFileSize(uri) < Const.MAX_FILE_SIZE) {
                startForegroundService(context, uri)
            } else {
                showFileSizeError(context, contentResolver.fileNameFromUri(uri))
            }
            _applicationState.emit(ApplicationState.EXIT)
        }
    }

    private fun startForegroundService(context: Context, uri: Uri) {
        val serviceIntent = Intent(context, FileUploaderForegroundService::class.java)
        serviceIntent.putExtra(Const.EXTRA_KEY_FILE, uri)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val APPLICATION_STATE_TIMEOUT_MILLIS: Long = 10_000
    }
}
