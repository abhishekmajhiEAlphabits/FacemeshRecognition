package com.example.facemeshrecognition

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.ImageFormat.NV21
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer


@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toYUV(): YuvImage? {
    val image = this.image ?: return null

    val imageBuffer = image.planes?.toNV21(this.width, this.height)
    val yuvImage = YuvImage(
        imageBuffer?.toByteArray(),
        ImageFormat.NV21,
        this.width,
        this.height, null
    )
    image.close()
    return yuvImage
}

@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toImageBuffer(): ByteBuffer? {
    val image = this.image ?: return null
    val imageBuffer = image.planes?.toNV21(this.width, this.height)
    image.close()
    return imageBuffer
}

@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toImage(): Image? {
    val image = this.image ?: return null
    image.close()
    return image
}

//fun Image.toBitmap(){
//    val imageBuffer = this.planes?.toNV21(this.width, this.height)
////    Log.d("ram","$this.width :: $this.height")
//}

fun ImageProxy.toYuvImage(image: Image): YuvImage? {
    require(!(image.format !== ImageFormat.YUV_420_888)) { "Invalid image format" }
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)

    // U and V are swapped
    yBuffer[nv21, 0, ySize]
    vBuffer[nv21, ySize, vSize]
    uBuffer[nv21, ySize + vSize, uSize]
    val width = image.width
    val height = image.height
    return YuvImage(nv21, NV21, width, height,  /* strides= */null)
}



//fun process(imageProxy: ImageProxy): ByteBuffer {
//    lateinit var streams: ByteBuffer
//    CoroutineScope(Dispatchers.IO).launch {
//        val yuv = imageProxy.toYUV()
//        val stream = ByteArrayOutputStream()
//        yuv!!.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 80, stream)
//        streams = ByteBuffer.wrap(stream.toByteArray())
//    }
//    return streams
//}

fun ImageProxy.toJpeg(compressionQuality: Int = 80): ByteBuffer? {
    val yuv = this.toYUV() ?: return null
    val stream = ByteArrayOutputStream()
    Log.d("hardik","Width ${this.width} Height ${this.height}")
    yuv.compressToJpeg(Rect(0, 0, this.width, this.height), compressionQuality, stream)
    return ByteBuffer.wrap(stream.toByteArray())
}

//fun ImageProxy.toBitmap(compressionQuality: Int = 80): Bitmap? {
////    val yuv = this.toYUV() ?: return null
//    val stream = ByteArrayOutputStream()
////    yuv.compressToJpeg(Rect(0, 0, this.width, this.height), compressionQuality, stream)
//    val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//    return bmp
//}

fun ByteArray.decodeToBitMap(): Bitmap? {
    var bmp: Bitmap? = null
    try {
        val image = YuvImage(this, ImageFormat.NV21, 640, 480, null)
        if (image != null) {
            Log.d("Yes", "image != null")
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, 640, 480), 80, stream)
            bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().size)
            stream.close()
        }
    } catch (ex: java.lang.Exception) {
        Log.e("No", "Error:" + ex.message)
    }
    return bmp
}

fun YuvImage.decodeToBitMap(image: YuvImage): Bitmap? {
    var bmp: Bitmap? = null
    try {
//        val image = YuvImage(this, ImageFormat.NV21, 640, 480, null)
        if (image != null) {
            Log.d("Yes", "image != null")
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, this.width, this.height), 10, stream)
//            Log.d("ram","${image.width} :: ${image.height} : ${stream.size()}")
//            val inputStream = ByteArrayInputStream(stream.toByteArray())
//            bmp = BitmapFactory.decodeStream(inputStream)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().size,options)
            stream.close()
        }
    } catch (ex: java.lang.Exception) {
        Log.e("No", "Error:" + ex.message)
    }
    return bmp
}

fun YuvImage.convertYuvImageToBitmap(yuvImage: YuvImage): Bitmap? {
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val imageBytes = out.toByteArray()
    try {
        out.close()
    } catch (e: IOException) {
        Log.e("No", "Exception while closing output stream", e)
    }
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Array<Image.Plane>.toNV21(width: Int, height: Int): ByteBuffer {


    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    if (BufferUtils.areUVPlanesNV21(this, width, height)) {
        // Copy the Y values.
        Log.d("abhi","If in....");
        this[0].buffer.get(out, 0, imageSize)
        val uBuffer: ByteBuffer = this[1].buffer
        val vBuffer: ByteBuffer = this[2].buffer
        // Get the first V value from the V buffer, since the U buffer does not contain it.
        vBuffer[out, imageSize, 1]
        // Copy the first U value and the remaining VU values from the U buffer.
        uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    } else {
        Log.d("abhi","If else....");
        // Fallback to copying the UV values one by one, which is slower but also works.
        // Unpack Y.
        BufferUtils.unpackPlane(this[0], width, height, out, 0, 1)
        // Unpack U.
        BufferUtils.unpackPlane(
            this[1],
            width,
            height,
            out,
            imageSize + 1,
            2
        )
        // Unpack V.
        BufferUtils.unpackPlane(
            this[2],
            width,
            height,
            out,
            imageSize,
            2
        )
    }

    return ByteBuffer.wrap(out)
}

fun ImageProxy.toByteArray(): ByteArray {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))

    this.planes[0].buffer.get(out, 0, imageSize)
    // Get the first V value from the V buffer, since the U buffer does not contain it.
    this.planes[2].buffer[out, imageSize, 1]
    // Copy the first U value and the remaining VU values from the U buffer.
    this.planes[1].buffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
//    this.close()
    return out
}

fun ByteBuffer.toByteArray(): ByteArray {
    try {
        rewind()
        if (hasArray())
            return this.array()
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes
    } catch (e: Exception) {
        return ByteArray(0)
    }
}

@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.getByteArray(image: ImageProxy): ByteArray? {
    image.image?.let {
        val nv21Buffer = yuv420ThreePlanesToNV21(
            it.planes, image.width, image.height
        )

        return ByteArray(nv21Buffer.remaining()).apply {
            nv21Buffer.get(this)
        }
    }

    return null
}

private fun yuv420ThreePlanesToNV21(
    yuv420888planes: Array<Image.Plane>,
    width: Int,
    height: Int
): ByteBuffer {
    val imageSize = width * height
    val out = ByteArray(imageSize + 2 * (imageSize / 4))
    if (areUVPlanesNV21(yuv420888planes, width, height)) {

        yuv420888planes[0].buffer[out, 0, imageSize]
        val uBuffer = yuv420888planes[1].buffer
        val vBuffer = yuv420888planes[2].buffer
        vBuffer[out, imageSize, 1]
        uBuffer[out, imageSize + 1, 2 * imageSize / 4 - 1]
    } else {
        unpackPlane(yuv420888planes[0], width, height, out, 0, 1)
        unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2)
        unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2)
    }
    return ByteBuffer.wrap(out)
}

private fun areUVPlanesNV21(planes: Array<Image.Plane>, width: Int, height: Int): Boolean {
    val imageSize = width * height
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val vBufferPosition = vBuffer.position()
    val uBufferLimit = uBuffer.limit()

    vBuffer.position(vBufferPosition + 1)
    uBuffer.limit(uBufferLimit - 1)

    val areNV21 =
        vBuffer.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

    vBuffer.position(vBufferPosition)
    uBuffer.limit(uBufferLimit)
    return areNV21
}

private fun unpackPlane(
    plane: Image.Plane,
    width: Int,
    height: Int,
    out: ByteArray,
    offset: Int,
    pixelStride: Int
) {
    val buffer = plane.buffer
    buffer.rewind()
    val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
    if (numRow == 0) {
        return
    }
    val scaleFactor = height / numRow
    val numCol = width / scaleFactor

    var outputPos = offset
    var rowStart = 0
    for (row in 0 until numRow) {
        var inputPos = rowStart
        for (col in 0 until numCol) {
            out[outputPos] = buffer[inputPos]
            outputPos += pixelStride
            inputPos += plane.pixelStride
        }
        rowStart += plane.rowStride
    }


}

fun ImageProxy.toNv21(image: Image): ByteArray? {
    val width: Int = image.getWidth()
    val height: Int = image.getHeight()

    // Order of U/V channel guaranteed, read more:
    // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
    val yPlane: Image.Plane = image.getPlanes().get(0)
    val uPlane: Image.Plane = image.getPlanes().get(1)
    val vPlane: Image.Plane = image.getPlanes().get(2)
    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer

    // Full size Y channel and quarter size U+V channels.
    val numPixels = (width * height * 1.5f).toInt()
    val nv21 = ByteArray(numPixels)
    var idY = 0
    var idUV = width * height
    val uvWidth = width / 2
    val uvHeight = height / 2

    // Copy Y & UV channel.
    // NV21 format is expected to have YYYYVU packaging.
    // The U/V planes are guaranteed to have the same row stride and pixel stride.
    val uvRowStride = uPlane.rowStride
    val uvPixelStride = uPlane.pixelStride
    val yRowStride = yPlane.rowStride
    val yPixelStride = yPlane.pixelStride
    for (y in 0 until height) {
        val yOffset = y * yRowStride
        val uvOffset = y * uvRowStride
        for (x in 0 until width) {
            nv21[idY++] = yBuffer[yOffset + x * yPixelStride]
            if (y < uvHeight && x < uvWidth) {
                val bufferIndex = uvOffset + x * uvPixelStride
                // V channel.
                nv21[idUV++] = vBuffer[bufferIndex]
                // U channel.
                nv21[idUV++] = uBuffer[bufferIndex]
            }
        }
    }
    return nv21
}