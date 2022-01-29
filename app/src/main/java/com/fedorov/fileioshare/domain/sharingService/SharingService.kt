package com.fedorov.fileioshare.domain.sharingService

import android.net.Uri
import com.fedorov.fileioshare.service.model.ResultState

interface SharingService {
    suspend fun sendFile(uri: Uri): ResultState
}
