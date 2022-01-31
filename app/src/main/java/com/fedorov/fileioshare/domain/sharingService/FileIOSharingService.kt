package com.fedorov.fileioshare.domain.sharingService

import android.net.Uri
import com.fedorov.fileioshare.Const
import com.fedorov.fileioshare.data.FileHandler
import com.fedorov.fileioshare.service.model.ResultState
import com.fedorov.fileioshare.utils.DispatcherProvider
import com.fedorov.fileioshare.utils.toRequestBody
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

class FileIOSharingService(
    private val client: OkHttpClient,
    private val fileHandler: FileHandler,
    private val dispatcherProvider: DispatcherProvider,
) : SharingService {
    override suspend fun sendFile(uri: Uri): ResultState {
        val fileName = fileHandler.fileNameFromUri(uri)
        return executeRequest(
            request = makeRequest(uri, fileName),
            fileName = fileName,
        ).getOrNull() ?: ResultState.Error
    }

    private fun makeRequest(fileUri: Uri, fileName: String): Request {
        val body = requestBody(fileUri, fileName)

        return Request.Builder()
            .url(API_ADDRESS)
            .post(body)
            .build()
    }

    private fun requestBody(fileUri: Uri, filename: String): MultipartBody {
        return MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                name = Const.FORM_DATA_PART_NAME,
                filename = filename,
                body = fileHandler.getFileInputStream(fileUri).toRequestBody(
                    mediaType = fileHandler.getType(fileUri).toMediaTypeOrNull(),
                )
            )
            .build()
    }

    private suspend fun executeRequest(request: Request, fileName: String): Result<ResultState?> {
        Timber.d("Send file started")
        return runCatching {
            withContext(dispatcherProvider.io) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.e(response.body?.string())

                        error("Unexpected code $response")
                    }

                    response.body?.string()?.let { responseBodyText ->
                        Timber.d(responseBodyText)

                        ResultState.Success(
                            url = JSONObject(responseBodyText).optString("link"),
                            fileName = fileName,
                        )
                    }
                }
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    companion object {
        private const val API_ADDRESS = "https://file.io"
    }
}
