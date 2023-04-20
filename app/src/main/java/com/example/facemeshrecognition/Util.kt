package com.example.facemeshrecognition

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class Util {

    fun process(imageProxy: ImageProxy): ByteBuffer {
        lateinit var streams: ByteBuffer
        CoroutineScope(Dispatchers.IO).launch {
            val yuv = imageProxy.toYUV()
            val stream = ByteArrayOutputStream()
            yuv!!.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, stream)
            streams = ByteBuffer.wrap(stream.toByteArray())
        }
        return streams
    }
}