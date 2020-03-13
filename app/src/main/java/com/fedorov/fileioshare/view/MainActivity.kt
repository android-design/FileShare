package com.fedorov.fileioshare.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.method.LinkMovementMethod
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

        presenter.continueProcessingWithIntent()
    }

    override fun handleIntent() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                setContentView(R.layout.uploading)

                (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                    grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                presenter.starting(intent, cacheDir)
            }
            else -> {
                setContentView(R.layout.activity_main)

                // Url to site file.io
                file_io_link.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    override fun finishActivity() {
        finish()
    }
}