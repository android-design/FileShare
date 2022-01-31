package com.fedorov.fileioshare.service.notification

import android.content.Context
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.utils.NotificationUtils

class NotificationForegroundImpl(private val context: Context) : NotificationForeground {
    override fun createNotificationChannel() {
        NotificationUtils.createNotificationChannel(context)
    }

    override fun foregroundNotification() = NotificationUtils.notificationBuilder(
        context,
        context.getString(R.string.app_name),
        context.getString(R.string.notification_uploading_content_text)
    ).build()
}
