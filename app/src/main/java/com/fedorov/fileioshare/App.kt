package com.fedorov.fileioshare

import android.app.Application
import android.content.Context
import com.fedorov.fileioshare.utils.DispatcherProvider
import timber.log.Timber

import timber.log.Timber.DebugTree


class App : Application() {
    val dispatcherProvider: DispatcherProvider = DispatcherProvider()

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
