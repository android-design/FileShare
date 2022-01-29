package com.fedorov.fileioshare.presentation.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.broadcastReceiver.FileIOBroadcastReceiver
import com.fedorov.fileioshare.utils.NotificationUtils
import timber.log.Timber

class NotificationFileImpl(
    private val context: Context,
) : NotificationFile {
    override fun sendSuccessNotification(url: String, filename: String) {
        val notificationId = NotificationUtils.notificationId()

        val builder = NotificationUtils.notificationGrouped(
            context = context,
            contentTitle = context.getString(R.string.notification_done_title, filename),
            contentText = context.getString(R.string.notification_done_content_text, url)
        )

        builder
            .addAction(
                R.drawable.ic_content_copy,
                context.getString(R.string.notification_action_copy_title),
                copyUrlPendingIntent(url, notificationId)
            )
            .addAction(
                R.drawable.ic_share,
                context.getString(R.string.notification_action_share_title),
                shareUrlPendingIntent(url, notificationId)
            )

        val groupSummary = NotificationUtils.notificationGroupBuilder(context)

        NotificationManagerCompat.from(context).apply {
            Timber.d("Notification about complete uploading sent")
            notify(notificationId, builder.build())
            notify(Const.GROUP_NOTIFICATION_ID, groupSummary.build())
        }
    }

    private fun shareUrlPendingIntent(
        url: String,
        notificationId: Int,
    ): PendingIntent? {
        val shareUrlIntent = Intent(context, FileIOBroadcastReceiver::class.java).apply {
            action = Const.ACTION_SHARE_URL
            putExtra(Const.EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            shareUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun copyUrlPendingIntent(
        url: String,
        notificationId: Int,
    ): PendingIntent? {
        val copyUrlIntent = Intent(context, FileIOBroadcastReceiver::class.java).apply {
            action = Const.ACTION_COPY_URL
            putExtra(Const.EXTRA_KEY_NOTIFICATION, url)
        }

        return PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            copyUrlIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun sendErrorNotification() {
        val notificationId = NotificationUtils.notificationId()

        val builder = NotificationUtils.notificationGrouped(
            context = context,
            contentTitle = context.getString(R.string.notification_error_title),
            contentText = context.getString(R.string.notification_done_error_content_text)
        )

        val groupSummary = NotificationUtils.notificationGroupBuilder(context)

        NotificationManagerCompat.from(context).apply {
            Timber.d("Notification about error while uploading")
            notify(notificationId, builder.build())
            notify(Const.GROUP_NOTIFICATION_ID, groupSummary.build())
        }
    }

    override fun createNotificationChannel() {
        NotificationUtils.createNotificationChannel(context)
    }

    override fun foregroundNotification() = NotificationUtils.notificationBuilder(
        context,
        context.getString(R.string.app_name),
        context.getString(R.string.notification_uploading_content_text)
    ).build()
}
