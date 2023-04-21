package com.example.facemeshrecognition

import android.annotation.TargetApi
import android.media.Image.Plane
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

object BufferUtils {
    /**
     * Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format.
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun areUVPlanesNV21(planes: Array<Plane>, width: Int, height: Int): Boolean {
        val imageSize = width * height
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        // Backup buffer properties.
        val vBufferPosition = vBuffer.position()
        val uBufferLimit = uBuffer.limit()

        // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
        vBuffer.position(vBufferPosition + 1)
        // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
        uBuffer.limit(uBufferLimit - 1)

        // Check that the buffers are equal and have the expected number of elements.
        val areNV21 =
            vBuffer.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

        // Restore buffers to their initial state.
        vBuffer.position(vBufferPosition)
        uBuffer.limit(uBufferLimit)
        return areNV21
    }

    /**
     * Unpack an image plane into a byte array.
     *
     *
     * The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
     * spaced by 'pixelStride'. Note that there is no row padding on the output.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun unpackPlane(
        plane: Plane, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int
    ) {
        val buffer = plane.buffer
        buffer.rewind()

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
        if (numRow == 0) {
            return
        }
        val scaleFactor = height / numRow
        val numCol = width / scaleFactor

        // Extract the data in the output buffer.
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

    fun unpackPlanes(
        planeBuffer: ByteBuffer,
        planeRowStride: Int,
        planePixelStride: Int,
//        plane: Plane,
        width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int
    ) {
        val buffer = planeBuffer
        buffer.rewind()

        // Compute the size of the current plane.
        // We assume that it has the aspect ratio as the original image.
        val numRow = (buffer.limit() + planeRowStride - 1) / planeRowStride
        if (numRow == 0) {
            return
        }
        val scaleFactor = height / numRow
        val numCol = width / scaleFactor

        // Extract the data in the output buffer.
        var outputPos = offset
        var rowStart = 0
        for (row in 0 until numRow) {
            var inputPos = rowStart
            for (col in 0 until numCol) {
                out[outputPos] = buffer[inputPos]
                outputPos += pixelStride
                inputPos += planePixelStride
            }
            rowStart += planeRowStride
        }
    }
}