package com.fedorov.fileioshare.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException
import java.io.InputStream


fun InputStream.toRequestBody(mediaType: MediaType?) =
    object : RequestBody() {
        override fun contentType(): MediaType? = mediaType

        override fun contentLength(): Long = try {
            available().toLong()
        } catch (e: IOException) {
            0
        }

        override fun writeTo(sink: BufferedSink) {
            use {
                sink.writeAll(source())
            }
        }
    }
