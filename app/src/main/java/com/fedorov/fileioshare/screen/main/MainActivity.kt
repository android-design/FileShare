package com.fedorov.fileioshare.screen.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.fedorov.fileioshare.R
import by.kirich1409.viewbindingdelegate.viewBinding
import com.fedorov.fileioshare.databinding.ActivityMainBinding
import com.fedorov.fileioshare.dispatcherProvider
import com.fedorov.fileioshare.screen.model.ApplicationState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


@FlowPreview
class MainActivity : AppCompatActivity() {

    private val viewModelFactory: ViewModelProvider.Factory by lazy(mode = LazyThreadSafetyMode.NONE) {
        MainViewModelFactory(
            applicationContext,
            applicationContext.dispatcherProvider,
        )
    }
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory
    }

    private val mainViewBinding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeOnAppState()
        checkStartFlow(intent)
    }

    private fun subscribeOnAppState() {
        viewModel.applicationState
            .onEach {
                when (it) {
                    ApplicationState.IDLE -> Unit
                    ApplicationState.EXIT -> {
                        finishAndRemoveTask()
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkStartFlow(intent)
    }

    private fun checkStartFlow(newIntent: Intent?) {
        val intent = newIntent ?: this.intent
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                setContentView(R.layout.uploading)
                uriFromIntent(intent)?.let { uri ->
                    startUploadingFile(listOf(uri))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                setContentView(R.layout.uploading)
                startUploadingFile(uriArrayFromIntent(intent))
            }
            else -> {
                setContentView(R.layout.activity_main)

                // Url to site file.io
                mainViewBinding.fileIoLink.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private fun uriFromIntent(intent: Intent): Uri? =
        intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri

    private fun uriArrayFromIntent(intent: Intent): List<Uri>? =
        intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)?.filterIsInstance<Uri>()

    private fun startUploadingFile(uriArray: List<Uri>?) {
        uriArray?.forEach {
            grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.startUploadingFile(applicationContext, it)
        }
    }
}
