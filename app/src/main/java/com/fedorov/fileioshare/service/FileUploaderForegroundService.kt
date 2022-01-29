package com.fedorov.fileioshare.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.broadcastReceiver.FileIOBroadcastReceiver
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.dispatcherProvider
import com.fedorov.fileioshare.utils.NotificationUtils
import com.fedorov.fileioshare.utils.setTextToClipboard
import com.fedorov.fileioshare.utils.toRequestBody
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class FileUploaderForegroundService : Service() {
    private val countUploading = AtomicInteger(0)
    private val defaultExceptionHandler = CoroutineExceptionHandler { _, t ->
        Timber.e(t)
    }

    private val serviceScope by lazy(LazyThreadSafetyMode.NONE) {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io + defaultExceptionHandler)
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    private val contentResolver: FileHandler by lazy(LazyThreadSafetyMode.NONE) {
        FileHandlerImpl(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        countUploading.incrementAndGet()

        val fileUri = intent.extras?.get(Const.EXTRA_KEY_FILE) as? Uri

        fileUri?.let { uri ->

            NotificationUtils.createNotificationChannel(this)

            val notification = NotificationUtils.notificationBuilder(
                this,
                getString(R.string.app_name),
                getString(R.string.notification_uploading_content_text)
            )

            startForeground(1, notification.build())

            Timber.d("Foreground service started")
            serviceScope.launch {
                val fileName = contentResolver.fileNameFromUri(uri)

                executeRequest(
                    request = makeRequest(uri, fileName),
                    fileName = fileName
                )
            }
        } ?: stopService()

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun makeRequest(fileUri: Uri, fileName: String): Request {
        val body = requestBody(fileUri, fileName)

        return Request.Builder()
            .url(Const.API_ADDRESS)
            .post(body)
            .build()
    }

    private fun requestBody(fileUri: Uri, filename: String): MultipartBody {
        return MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                name = Const.FORM_DATA_PART_NAME,
                filename = filename,
                body = contentResolver.getFileInputStream(fileUri).toRequestBody(
                    mediaType = contentResolver.getType(fileUri).toMediaTypeOrNull(),
                )
            )
            .build()
    }

    private suspend fun executeRequest(request: Request, fileName: String) {
        Timber.d("Send file started")
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e(response.body?.string())

                    error("Unexpected code $response")
                }

                response.body?.string()?.let { responseBodyText ->
                    Timber.d(responseBodyText)

                    val url = JSONObject(responseBodyText).optString("link")

                    withContext(dispatcherProvider.foreground) {
                        setTextToClipboard(
                            this@FileUploaderForegroundService,
                            url
                        )

                        sendFinishedNotification(url = url, fileName = fileName)
                    }
                }
            }
        }
            .onFailure {
                Timber.e(it)
            }
            .onSuccess {
                stopService()
            }
    }

    private fun sendFinishedNotification(
        url: String = "",
        fileName: String
    ) {
        val notificationId = NotificationUtils.notificationId()

        val builder = NotificationUtils.notificationGrouped(
            context = this,
            contentTitle = getString(R.string.notification_done_title, fileName),
            contentText = getString(R.string.notification_done_content_text, url)
        )

        builder
            .addAction(
                R.drawable.ic_content_copy,
                getString(R.string.notification_action_copy_title),
                copyUrlPendingIntent(url, notificationId)
            )
            .addAction(
                R.drawable.ic_share,
                getString(R.string.notification_action_share_title),
                shareUrlPendingIntent(url, notificationId)
            )

        val groupSummary = NotificationUtils.notificationGroupBuilder(context = this)

        NotificationManagerCompat.from(this).apply {
            Timber.d("Notification about complete uploading sent")
            notify(notificationId, builder.build())
            notify(Const.GROUP_NOTIFICATION_ID, groupSummary.build())
        }
    }

    private fun shareUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val shareUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
            action = Const.ACTION_SHARE_URL
            putExtra(Const.EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            this,
            notificationId + 2,
            shareUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun copyUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val copyUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
            action = Const.ACTION_COPY_URL
            putExtra(Const.EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            this,
            notificationId + 1,
            copyUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
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
