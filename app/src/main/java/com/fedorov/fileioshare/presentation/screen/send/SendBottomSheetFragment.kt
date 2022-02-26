package com.fedorov.fileioshare.presentation.screen.send

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.kirich1409.viewbindingdelegate.viewBinding
import com.fedorov.fileioshare.R
import com.fedorov.fileioshare.databinding.SendBottomSheetContentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SendBottomSheetFragment : BottomSheetDialogFragment() {

    private val binding by viewBinding(SendBottomSheetContentBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.send_bottom_sheet_content, container, false)

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finishAndRemoveTask()
    }

    fun changeTextToSent() {
        binding.sendTv.setText(R.string.send_text)
    }

    companion object {
        fun newInstance() = SendBottomSheetFragment()
        const val TAG = "SendBottomSheetFragment"
    }
}
