package com.fedorov.fileioshare.presentation.screen.send

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.databinding.ActivitySendBinding
import com.fedorov.fileioshare.dispatcherProvider
import com.fedorov.fileioshare.model.ApplicationState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@FlowPreview
class SendActivity : AppCompatActivity(R.layout.activity_send) {

    private val viewModelFactory: ViewModelProvider.Factory by lazy(mode = LazyThreadSafetyMode.NONE) {
        SendViewModelFactory(
            applicationContext,
            applicationContext.dispatcherProvider,
        )
    }

    private val viewModel: SendViewModel by viewModels {
        viewModelFactory
    }

    private val binding by viewBinding(ActivitySendBinding::bind)

    private var sendBottomSheet: SendBottomSheetFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        subscribeOnAppState()
        checkStartFlow(intent)
    }

    @FlowPreview
    private fun subscribeOnAppState() {
        lifecycleScope.launchWhenCreated {
            viewModel.applicationState
                .onEach {
                    when (it) {
                        ApplicationState.IDLE -> Unit
                        ApplicationState.SENT -> {
                            sendBottomSheet?.changeTextToSent()
                        }
                        ApplicationState.EXIT -> {
                            finishAndRemoveTask()
                        }
                    }
                }
                .launchIn(lifecycleScope)
        }
    }

    override fun onStart() {
        super.onStart()
        SendBottomSheetFragment
            .newInstance().also {
                sendBottomSheet = it
                it.show(supportFragmentManager, SendBottomSheetFragment.TAG)
            }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkStartFlow(intent)
    }

    override fun onPause() {
        finishAndRemoveTask()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBottomSheet = null
    }

    private fun checkStartFlow(newIntent: Intent?) {
        val intent = newIntent ?: this.intent
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                uriFromIntent(intent)?.let { uri ->
                    startUploadingFile(listOf(uri))
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                startUploadingFile(uriArrayFromIntent(intent))
            }
            else -> Timber.d(IllegalArgumentException(intent?.action))
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

    private fun hideSystemUI() {
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(binding.root) ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
