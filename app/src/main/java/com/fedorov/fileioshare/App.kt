package com.fedorov.fileioshare

import android.app.Application
import android.content.Context
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.domain.sharingService.FileIOSharingService
import com.fedorov.fileioshare.domain.sharingService.SharingService
import com.fedorov.fileioshare.presentation.notification.NotificationFile
import com.fedorov.fileioshare.presentation.notification.NotificationFileImpl
import com.fedorov.fileioshare.utils.DispatcherProvider
import okhttp3.OkHttpClient
import timber.log.Timber
import timber.log.Timber.DebugTree

class App : Application() {
    val dispatcherProvider: DispatcherProvider = DispatcherProvider()
    val sharingService: SharingService by lazy {
        FileIOSharingService(
            client = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build(),
            fileHandler = FileHandlerImpl(this),
            dispatcherProvider = dispatcherProvider,
        )
    }
    val notification: NotificationFile by lazy {
        NotificationFileImpl(
            context = applicationContext,
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }
}

val Context.dispatcherProvider: DispatcherProvider
    get() = when (this) {
        is App -> this.dispatcherProvider
        else -> applicationContext.dispatcherProvider
    }

val Context.sharingService: SharingService
    get() = when (this) {
        is App -> this.sharingService
        else -> applicationContext.sharingService
    }

val Context.notification: NotificationFile
    get() = when (this) {
        is App -> this.notification
        else -> applicationContext.notification
    }
