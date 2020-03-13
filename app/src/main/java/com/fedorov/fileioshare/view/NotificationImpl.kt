package com.fedorov.fileioshare.view

import android.content.Context
import androidx.core.app.NotificationCompat
import com.fedorov.fileioshare.CHANNEL_ID
import com.fedorov.fileioshare.GROUP_KEY_UPLOAD_FILE
import com.fedorov.fileioshare.R

object NotificationImpl {
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
}