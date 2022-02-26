package com.fedorov.fileioshare.presentation.screen.main

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.databinding.ActivityMainBinding
import kotlinx.coroutines.FlowPreview

@FlowPreview
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private val mainViewBinding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Url to site file.io
        mainViewBinding.fileIoLink.movementMethod = LinkMovementMethod.getInstance()
    }
}
