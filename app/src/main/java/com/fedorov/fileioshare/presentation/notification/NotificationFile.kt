package com.fedorov.fileioshare.presentation.notification

interface NotificationFile {
    fun sendSuccessNotification(url: String, filename: String)
    fun sendErrorNotification()
}
