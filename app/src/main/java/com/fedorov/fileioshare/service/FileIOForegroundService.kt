package com.fedorov.fileioshare.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.*
import com.fedorov.fileioshare.broadcastReceiver.FileIOBroadcastReceiver
import com.fedorov.fileioshare.utils.setTextToClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException


class FileIOForegroundService : Service() {

    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val file = intent.extras?.get(ACTION_INTENT_KEY) as? File
//        val type = intent.getStringExtra("TYPE")

        file?.let {
            createNotificationChannel()

            val notification =
                NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                    )
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_uploading_content_text))
                    .setSmallIcon(R.drawable.folder_bw)
                    .build()

            startForeground(1, notification)

            try {
                handleSendFile(file)
            } catch (e: Exception) {
                stopSelf()
            }

        } ?: stopSelf()

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
        }
    }

    private fun handleSendFile(file: File) {
        GlobalScope.launch(Dispatchers.Default) {
            makeRequest(file)
            stopSelf()
        }
    }

    private suspend fun makeRequest(file: File) {
        val requestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", file.name,
                    file.asRequestBody(null)
                )
                .build()

        val request =
            Request.Builder()
                .url(API_ADDRESS)
                .post(requestBody)
                .build()

        @Suppress("BlockingMethodInNonBlockingContext")
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                sendFinishedNotification(fileName = file.name, isError = true)
                throw IOException("Unexpected code $response")
            }

            response.body?.string()?.let {
                val url = JSONObject(it).optString("link")
                withContext(Dispatchers.Main) {
                    setTextToClipboard(
                        this@FileIOForegroundService,
                        url
                    )
                    sendFinishedNotification(url = url, fileName = file.name)
                }
            }
        }
    }

    private fun sendFinishedNotification(
        url: String = "",
        fileName: String,
        isError: Boolean = false
    ) {

        val notificationId = notificationId()

        val builder = NotificationCompat.Builder(
                this@FileIOForegroundService,
                CHANNEL_ID
            )
            .setSmallIcon(R.drawable.folder_bw)
            .setContentTitle(getString(R.string.notification_done_title, fileName))
            .setGroup("GROUP_KEY_UPLOAD_FILE")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (!isError) {
            val copyUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
                action = ACTION_COPY_URL
                putExtra(EXTRA_KEY_NOTIFICATION, url)
            }

            val shareUrlIntent = Intent(this, FileIOBroadcastReceiver::class.java).apply {
                action = ACTION_SHARE_URL
                putExtra(EXTRA_KEY_NOTIFICATION, url)
            }

            val copyUrlPendingIntent =
                PendingIntent.getBroadcast(this, notificationId + 1, copyUrlIntent, 0)

            val shareUrlPendingIntent =
                PendingIntent.getBroadcast(this, notificationId + 2, shareUrlIntent, 0)

            builder
                .setContentText(getString(R.string.notification_done_content_text, url))
                .addAction(
                    R.drawable.ic_content_copy, getString(
                        R.string.notification_action_copy_title
                    ),
                    copyUrlPendingIntent
                )
                .addAction(
                    R.drawable.ic_share, getString(
                        R.string.notification_action_share_title
                    ),
                    shareUrlPendingIntent
                )
        } else {
            builder
                .setContentText(getString(R.string.notification_done_error_content_text))
        }

        with(NotificationManagerCompat.from(this@FileIOForegroundService)) {
            notify(notificationId, builder.build())
        }

        val groupSummary = NotificationCompat.Builder(
                this@FileIOForegroundService,
                CHANNEL_ID
            )
            .setSmallIcon(R.drawable.folder_bw)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .setGroup("GROUP_KEY_UPLOAD_FILE")

        NotificationManagerCompat.from(this).apply {
            notify(notificationId, builder.build())
            notify(3, groupSummary.build())
        }
    }

    private fun notificationId() = SystemClock.uptimeMillis().toInt()
}