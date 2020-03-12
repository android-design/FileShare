package com.fedorov.fileioshare.view

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fedorov.fileioshare.PERMISSIONS_REQUEST_READ_STORAGE
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.presenter.MainPresenter
import kotlinx.android.synthetic.main.activity_main.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter


class MainActivity : MvpAppCompatActivity(), MainView {

    private val presenter by moxyPresenter {
        MainPresenter(
            applicationContext,
            FileHandlerImpl(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                PERMISSIONS_REQUEST_READ_STORAGE
            )
        } else {
            // Permission has already been granted
            presenter.continueProcessingWithIntent()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    presenter.continueProcessingWithIntent()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun handleIntent() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                    grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                presenter.starting(intent, cacheDir)
            }
            else -> {
                // Url to site file.io
                file_io_link.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    override fun finishActivity() {
        finish()
    }
}