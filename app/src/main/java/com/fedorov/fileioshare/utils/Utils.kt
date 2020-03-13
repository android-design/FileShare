package com.fedorov.fileioshare.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
import com.fedorov.fileioshare.CLIPBOARD_LABEL

fun setTextToClipboard(context: Context, text: String?) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip =
        ClipData.newPlainText(
            CLIPBOARD_LABEL,
            text
        )
    clipboard.setPrimaryClip(clip)
}

fun notificationId() = SystemClock.uptimeMillis().toInt()