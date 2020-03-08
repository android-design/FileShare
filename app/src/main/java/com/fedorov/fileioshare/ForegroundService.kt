package com.fedorov.fileioshare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException


class ForegroundService : Service() {

    private val client by lazy { OkHttpClient() }
    private var fileName = ""
    private var fileSize = 0
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val actionIntent = (intent.extras?.get(ACTION_INTENT_KEY) as? Intent)
        actionIntent?.let {
            createNotificationChannel()

            val notification =
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.notification_uploading_content_text))
                    .setSmallIcon(R.drawable.folder)
                    .build()

            startForeground(1, notification)

            try {
                handleSendFile(it)
            } catch (e: Exception) {
                stopSelf()
            }

        } ?: stopSelf()

        return START_NOT_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService<NotificationManager>(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun handleSendFile(intent: Intent) {
        (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
            ?.let { uri ->
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->

                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    cursor.moveToFirst()
                    fileName = cursor.getString(nameIndex)
                    fileSize = cursor.getInt(sizeIndex)
                }

                if (fileName.isEmpty()) {
                    fileName = uri.path?.substringAfterLast("/") ?: ""
                }


                if (fileName.isNotEmpty()) {
                    contentResolver.openInputStream(uri)
                        ?.let { stream ->
                            val type = contentResolver.getType(uri)
                            val file = stream.toFile(fileName, cacheDir)

                            if (file.length() < 5_368_709_120) {
                                makeRequest(file, type)
                            }
                            else{
                                throw IllegalArgumentException(getString(R.string.exception_size))
                            }
                        }
                } else {
                    throw IllegalArgumentException(getString(R.string.exception_filename))
                }
                stopSelf()
            }
    }

    private fun makeRequest(file: File, type: String?) {
        val requestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", file.name,
                    file.asRequestBody(type?.toMediaTypeOrNull())
                )
                .build()

        val request =
            Request.Builder()
                .url(API_ADDRESS)
                .post(requestBody)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) throw IOException("Unexpected code $r")

                    r.body?.string()?.let {
                        val link = JSONObject(it).optString("link")

                        handler.post {
                            copyUrlToClipboard(link)
                            sendFinishedNotification(link)
                        }
                    }
                }
            }
        }
        )
    }

    private fun copyUrlToClipboard(link: String) {

        val clipboard =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip =
            ClipData.newPlainText(
                CLIPBOARD_LABEL,
                link
            )
        clipboard.setPrimaryClip(clip)
    }

    private fun sendFinishedNotification(link: String) {

        val builder = NotificationCompat.Builder(
            this@ForegroundService,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.folder)
            .setContentTitle(getString(R.string.notification_done_title))
            .setContentText(getString(R.string.notification_done_content_text, link))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this@ForegroundService)) {
            // notificationId is a unique int for each notification that you must define
            notify(link, 2, builder.build())
        }
    }
}