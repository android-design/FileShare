package com.fedorov.fileioshare.presentation.notification

import android.app.Notification

interface NotificationFile {
    fun sendSuccessNotification(url: String, filename: String)
    fun sendErrorNotification()
    fun createNotificationChannel()
    fun foregroundNotification(): Notification
}
