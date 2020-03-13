package com.fedorov.fileioshare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.*
import com.fedorov.fileioshare.broadcastReceiver.FileIOBroadcastReceiver
import com.fedorov.fileioshare.utils.notificationId
import com.fedorov.fileioshare.utils.setTextToClipboard
import com.fedorov.fileioshare.view.NotificationImpl
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger


class FileUploaderForegroundService : Service() {

    private val countUploading = AtomicInteger(0)

    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        countUploading.incrementAndGet()

        val file = intent.extras?.get(EXTRA_KEY_FILE) as? File

        file?.let {
            createNotificationChannel()

            val notification = NotificationImpl.notificationBuilder(
                this,
                getString(R.string.app_name),
                getString(R.string.notification_uploading_content_text)
            )

            startForeground(1, notification.build())

            Timber.i("Foreground service start")
            GlobalScope.launch(Dispatchers.Default) {
                try {
                    val type = intent.getStringExtra(EXTRA_KEY_TYPE)
                    handleSendFile(file, type)

                } catch (e: Exception) {
                    Timber.e(e)
                } finally {
                    stopService()
                }
            }

        } ?: stopService()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
            Timber.i("Notification chanel created")
        }
    }

    private suspend fun handleSendFile(file: File, type: String?) {
        makeRequest(file, type)
    }

    private suspend fun makeRequest(file: File, type: String?) {
        val body = requestBody(file, type)

        val request =
            Request.Builder()
                .url(API_ADDRESS)
                .post(body)
                .build()

        executeRequest(request, file)
    }

    private suspend fun executeRequest(request: Request, file: File) {
        Timber.i("Send file started")

        @Suppress("BlockingMethodInNonBlockingContext")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.e(response.body?.string())

                throw IOException("Unexpected code $response")
            }

            response.body?.string()?.let { responseBodyText ->
                Timber.i(responseBodyText)

                val url = JSONObject(responseBodyText).optString("link")

                withContext(Dispatchers.Main) {
                    setTextToClipboard(
                        this@FileUploaderForegroundService,
                        url
                    )

                    sendFinishedNotification(url = url, fileName = file.name)
                }
            }
        }
    }

    private fun requestBody(file: File, type: String?): MultipartBody {
        return MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                FORM_DATA_PART_NAME, file.name,
                file.asRequestBody(type?.toMediaTypeOrNull())
            )
            .build()
    }

    private fun sendFinishedNotification(
        url: String = "",
        fileName: String
    ) {
        val notificationId = notificationId()

        val builder = NotificationImpl.notificationGrouped(
            context = this,
            contentTitle = getString(R.string.notification_done_title, fileName),
            contentText = getString(R.string.notification_done_content_text, url)
        )

        builder
            .addAction(
                R.drawable.ic_content_copy, getString(
                    R.string.notification_action_copy_title
                ),
                copyUrlPendingIntent(url, notificationId)
            )
            .addAction(
                R.drawable.ic_share, getString(
                    R.string.notification_action_share_title
                ),
                shareUrlPendingIntent(url, notificationId)
            )

        val groupSummary = NotificationImpl.notificationGroupBuilder(context = this)

        NotificationManagerCompat.from(this).apply {
            Timber.i("Notification about complete uploading sent")
            notify(notificationId, builder.build())
            notify(3, groupSummary.build())
        }
    }

    private fun shareUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val shareUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
            action = ACTION_SHARE_URL
            putExtra(EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(this, notificationId + 2, shareUrlIntent, 0)
    }

    private fun copyUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val copyUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
            action = ACTION_COPY_URL
            putExtra(EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(this, notificationId + 1, copyUrlIntent, 0)
    }

    private fun stopService() {
        // Stop service only when it is last uploading process.
        if (countUploading.decrementAndGet() == 0) {
            Timber.i("Service stopped")

            stopSelf()
        }
    }
}