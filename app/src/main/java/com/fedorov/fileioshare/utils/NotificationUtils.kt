package com.fedorov.fileioshare.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.R
import timber.log.Timber

object NotificationUtils {
    fun notificationBuilder(
        context: Context,
        contentTitle: String,
        contentText: String = "",
    ): NotificationCompat.Builder = NotificationCompat.Builder(
        context,
        Const.CHANNEL_ID
    )
        .setSmallIcon(R.drawable.folder_bw)
        .setContentTitle(contentTitle)
        .setTicker(contentTitle)
        .setContentText(contentText)

    fun notificationGrouped(
        context: Context,
        contentTitle: String,
        contentText: String = "",
    ): NotificationCompat.Builder =
        notificationBuilder(
            context,
            contentTitle,
            contentText
        ).setGroup(Const.GROUP_KEY_UPLOAD_FILE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    fun notificationGroupBuilder(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(
            context,
            Const.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.folder_bw)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
            .setGroup(Const.GROUP_KEY_UPLOAD_FILE)

    fun showFileSizeError(context: Context, fileName: String) {
        val builder = notificationGrouped(
            context = context,
            contentTitle = "Error. $fileName, " + context.getString(R.string.error_file_size)
        )
        NotificationManagerCompat.from(context).apply {
            notify(notificationId(), builder.build())
        }
    }

    fun notificationId() = SystemClock.uptimeMillis().toInt()

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                Const.CHANNEL_ID,
                Const.CHANNEL_NAME,
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
