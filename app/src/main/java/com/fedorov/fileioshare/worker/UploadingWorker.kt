package com.fedorov.fileioshare.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.fedorov.fileioshare.*
import com.fedorov.fileioshare.broadcastReceiver.FileIOBroadcastReceiver
import com.fedorov.fileioshare.utils.Notification
import com.fedorov.fileioshare.utils.notificationId
import com.fedorov.fileioshare.utils.setTextToClipboard
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File

class UploadingWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    private val client by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    override suspend fun doWork(): Result {
        val fileName = inputData.getString(EXTRA_KEY_FILE) ?: return Result.failure()
        val type = inputData.getString(EXTRA_KEY_TYPE) ?: return Result.failure()

        setForeground(createForegroundInfo())

        val file = File(applicationContext.cacheDir, fileName)
        return executeRequest(
            request = makeRequest(file, type),
            file = file
        )
    }

    private fun makeRequest(file: File, type: String?): Request {
        val body = requestBody(file, type)

        return Request.Builder()
            .url(API_ADDRESS)
            .post(body)
            .build()
    }

    private fun requestBody(file: File, type: String?): MultipartBody {
        return MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                FORM_DATA_PART_NAME, file.name,
                file.asRequestBody(type?.toMediaTypeOrNull())
            )
            .build()
    }

    private suspend fun executeRequest(request: Request, file: File): Result {
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

                    withContext(applicationContext.dispatcherProvider.foreground) {
                        setTextToClipboard(
                            applicationContext,
                            url
                        )

                        sendFinishedNotification(url = url, fileName = file.name)
                    }
                }
            }
        }
            .onFailure {
                Timber.e(it)
                return Result.failure()
            }
            .onSuccess {
                return Result.success()
            }
        return Result.success()
    }

    private fun sendFinishedNotification(
        url: String = "",
        fileName: String
    ) {
        val notificationId = notificationId()

        val builder = Notification.notificationGrouped(
            context = applicationContext,
            contentTitle = applicationContext.getString(R.string.notification_done_title, fileName),
            contentText = applicationContext.getString(
                R.string.notification_done_content_text,
                url
            ),
        )

        builder
            .addAction(
                R.drawable.ic_content_copy, applicationContext.getString(
                    R.string.notification_action_copy_title
                ),
                copyUrlPendingIntent(url, notificationId)
            )
            .addAction(
                R.drawable.ic_share, applicationContext.getString(
                    R.string.notification_action_share_title
                ),
                shareUrlPendingIntent(url, notificationId)
            )

        val groupSummary = Notification.notificationGroupBuilder(applicationContext)

        with(notificationManager) {
            Timber.d("Notification about complete uploading sent")
            notify(notificationId, builder.build())
            notify(3, groupSummary.build())
        }
    }

    private fun shareUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val shareUrlIntent = Intent(applicationContext, FileIOBroadcastReceiver::class.java).apply {
            action = ACTION_SHARE_URL
            putExtra(EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 2,
            shareUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun copyUrlPendingIntent(
        url: String,
        notificationId: Int
    ): PendingIntent? {
        val copyUrlIntent = Intent(applicationContext, FileIOBroadcastReceiver::class.java).apply {
            action = ACTION_COPY_URL
            putExtra(EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 1,
            copyUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createForegroundInfo(): ForegroundInfo {
        Notification.createNotificationChannel(applicationContext)

        val notification = Notification.notificationBuilder(
            context = applicationContext,
            contentTitle = applicationContext.getString(R.string.app_name),
            contentText = applicationContext.getString(R.string.notification_uploading_content_text),
        )
            .build()

        return ForegroundInfo(1, notification)
    }
}
