package com.fedorov.fileioshare.service.notification

import android.app.Notification

interface NotificationForeground {
    fun createNotificationChannel()
    fun foregroundNotification(): Notification
}
