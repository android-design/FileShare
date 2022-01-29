package com.fedorov.fileioshare.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.dispatcherProvider
import com.fedorov.fileioshare.notification
import com.fedorov.fileioshare.service.model.ResultState
import com.fedorov.fileioshare.sharingService
import com.fedorov.fileioshare.utils.setTextToClipboard
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class FileUploaderForegroundService : Service() {
    private val defaultExceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }

    private val serviceScope by lazy(LazyThreadSafetyMode.NONE) {
        CoroutineScope(SupervisorJob() + dispatcherProvider.background + defaultExceptionHandler)
    }

    private val countUploading = AtomicInteger()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        countUploading.incrementAndGet()

        val fileUri = intent.extras?.get(Const.EXTRA_KEY_FILE) as? Uri

        fileUri?.let { uri ->
            notification.createNotificationChannel()

            startForeground(1, notification.foregroundNotification())
            Timber.d("Foreground service started")

            serviceScope.launch {
                val asyncSendFile = async {
                    sharingService.sendFile(uri)
                }

                when (val result = asyncSendFile.await()) {
                    ResultState.Error -> notification.sendErrorNotification()
                    is ResultState.Success -> {
                        notification.sendSuccessNotification(
                            url = result.url,
                            filename = result.fileName,
                        )
                        copyUrlToClipboard(result.url)
                    }
                }
                stopService()
            }
        } ?: run {
            notification.sendErrorNotification()
            stopService()
        }

        return START_REDELIVER_INTENT
    }

    private suspend fun copyUrlToClipboard(url: String) {
        withContext(dispatcherProvider.foreground) {
            setTextToClipboard(
                this@FileUploaderForegroundService,
                url,
            )
        }
    }

    private fun stopService() {
        // Stop service only when it is last uploading process.
        if (countUploading.decrementAndGet() == 0) {
            Timber.d("Service stopped")
            serviceScope.cancel()
            stopSelf()
        }
    }
}
