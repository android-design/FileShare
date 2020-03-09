package com.fedorov.fileioshare.broadcastReceiver

import android.content.*
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

                Toast.makeText(
                    context,
                    context.getString(R.string.toast_url_copied),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            ACTION_SHARE_URL -> {
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, url)
                    type = "text/plain"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(sendIntent)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_no_app),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
    }
}