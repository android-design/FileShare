package com.fedorov.fileioshare.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fedorov.fileioshare.ACTION_INTENT_KEY
import com.fedorov.fileioshare.PERMISSIONS_REQUEST_READ_STORAGE
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.data.FileHandlerImpl
import com.fedorov.fileioshare.presenter.MainPresenter
import com.fedorov.fileioshare.service.FileIOForegroundService
import kotlinx.android.synthetic.main.activity_main.*
import moxy.MvpAppCompatActivity
import moxy.ktx.moxyPresenter
import java.io.File


class MainActivity : MvpAppCompatActivity(), MainView {

    private val presenter by moxyPresenter { MainPresenter(FileHandlerImpl(applicationContext)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        file_io_link.movementMethod = LinkMovementMethod.getInstance()

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
                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)
                    ?.let { uri ->
                        presenter.getFileFromUri(uri, cacheDir)
                    }
            }
        }
    }

    override fun showError(errorMsg: String) {
        Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT).show()
    }

    override fun showMsg(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    override fun startForegroundService(file: File) {
        val serviceIntent = Intent(this, FileIOForegroundService::class.java)
        serviceIntent.putExtra(ACTION_INTENT_KEY, file)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    override fun finishActivity() {
        finish()
    }
}