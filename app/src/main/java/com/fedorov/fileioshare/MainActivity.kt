package com.fedorov.fileioshare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    lateinit var actionIntent: Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionIntent = intent

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_STORAGE
            )
        } else {
            // Permission has already been granted
            continuesAction(actionIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    continuesAction(actionIntent)
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun continuesAction(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val serviceIntent = Intent(this, ForegroundService::class.java)
                serviceIntent.putExtra(ACTION_INTENT_KEY, intent)
                ContextCompat.startForegroundService(this, serviceIntent)
                finish()
            }
            else -> {}
        }
    }
}