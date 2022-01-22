package com.fedorov.fileioshare.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.CHANNEL_ID
import com.fedorov.fileioshare.CHANNEL_NAME
import com.fedorov.fileioshare.GROUP_KEY_UPLOAD_FILE
import com.fedorov.fileioshare.R
import timber.log.Timber

object Notification {
    fun notificationBuilder(
        context: Context,
        contentTitle: String,
        contentText: String = ""
    ): NotificationCompat.Builder = NotificationCompat.Builder(
        context,
        CHANNEL_ID
    )
        .setSmallIcon(R.drawable.folder_bw)
        .setContentTitle(contentTitle)
        .setTicker(contentTitle)
        .setContentText(contentText)

    fun notificationGrouped(
        context: Context,
        contentTitle: String,
        contentText: String = ""
    ): NotificationCompat.Builder =
        notificationBuilder(context, contentTitle, contentText).setGroup(GROUP_KEY_UPLOAD_FILE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    fun notificationGroupBuilder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.folder_bw)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .setGroup(GROUP_KEY_UPLOAD_FILE)

    fun showFileSizeError(context: Context, fileName: String) {
        val builder = notificationGrouped(
            context = context,
            contentTitle = "Error. $fileName, " + context.getString(R.string.error_file_size)
        )
        NotificationManagerCompat.from(context).apply {
            notify(notificationId(), builder.build())
        }
    }

    fun showNotificationError(context: Context) {
        val builder = notificationGrouped(
            context = context,
            contentTitle = "Error. " + context.getString(R.string.error_can_not_read_file)
        )
        NotificationManagerCompat.from(context).apply {
            notify(notificationId(), builder.build())
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(
                NotificationManager::class.java
            )
            manager?.createNotificationChannel(serviceChannel)
            Timber.d("Notification chanel created")
        }
    }
}
