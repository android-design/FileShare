package com.fedorov.fileioshare.service.model

sealed class ResultState {
    class Success(
        val url: String,
        val fileName: String,
    ) : ResultState()

    object Error : ResultState()
}
