package com.fedorov.fileioshare.broadcastReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.fedorov.fileioshare.ACTION_COPY_URL
import com.fedorov.fileioshare.ACTION_SHARE_URL
import com.fedorov.fileioshare.EXTRA_KEY_NOTIFICATION
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.utils.setTextToClipboard


class FileIOBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra(EXTRA_KEY_NOTIFICATION)

        when (intent.action) {
            ACTION_COPY_URL -> {
                setTextToClipboard(
                    context,
                    url
                )

                showToast(context, context.getString(R.string.toast_url_copied))
            }

            ACTION_SHARE_URL -> {
                val shareTextIntent = getShareTextIntent(url)

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(shareTextIntent)
                } else {
                    showToast(context, context.getString(R.string.toast_no_app))
                }
            }
        }
    }

    private fun getShareTextIntent(url: String?) = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    private fun showToast(context: Context, text: String) {
        Toast.makeText(
            context,
            text,
            Toast.LENGTH_LONG
        )
            .show()
    }
}
