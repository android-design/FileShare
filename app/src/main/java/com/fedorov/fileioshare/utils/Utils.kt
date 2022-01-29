package com.fedorov.fileioshare.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.fedorov.fileioshare.Const

fun setTextToClipboard(context: Context, text: String?) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip =
        ClipData.newPlainText(
            Const.CLIPBOARD_LABEL,
            text
        )
    clipboard.setPrimaryClip(clip)
}
