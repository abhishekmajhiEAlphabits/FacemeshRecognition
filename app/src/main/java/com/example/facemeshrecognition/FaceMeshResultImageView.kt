package com.example.facemeshrecognition

import android.content.Context
import android.graphics.*
import android.util.Size
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import com.google.common.collect.ImmutableSet
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class FaceMeshResultImageView(context: Context?) : AppCompatImageView(context) {
    private lateinit var latest: Bitmap
    var bitmapCounter = 0
    var toastCounter = 0

    /**
     * Sets a [FaceMeshResult] to render.
     *
     * @param result a [FaceMeshResult] object that contains the solution outputs and the input
     * [Bitmap].
     */
    fun setFaceMeshResult(result: FaceMeshResult?) {
        if (result == null) {
            return
        }
        val bmInput = result.inputBitmap()
        val width = bmInput.width
        val height = bmInput.height
        latest = Bitmap.createBitmap(width, height, bmInput.config)
        val canvas = Canvas(latest)
        val imageSize = Size(width, height)
        canvas.drawBitmap(bmInput, Matrix(), null)
        val foreHead1: LandmarkProto.NormalizedLandmark =
            result.multiFaceLandmarks()[0].landmarkList[107]
        val foreHead2: LandmarkProto.NormalizedLandmark =
            result.multiFaceLandmarks()[0].landmarkList[108]
        if (++bitmapCounter % 2 == 0) {
            val numFaces = result.multiFaceLandmarks().size
            if (numFaces == null) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context,"No Face Detected",Toast.LENGTH_LONG).show()
                }
            }
            for (i in 0 until numFaces) {
                drawLandmarks(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    Roi.foreHead,
                    imageSize,
                    FACE_OVAL_COLOR,
                    FACE_OVAL_THICKNESS
                )
                drawLandmarks(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    Roi.leftCheek,
                    imageSize,
                    FACE_OVAL_COLOR,
                    FACE_OVAL_THICKNESS
                )
                drawLandmarks(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    Roi.rightCheek,
                    imageSize,
                    FACE_OVAL_COLOR,
                    FACE_OVAL_THICKNESS
                )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_TESSELATION,
////                imageSize,
////                TESSELATION_COLOR,
////                TESSELATION_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_RIGHT_EYE,
////                imageSize,
////                RIGHT_EYE_COLOR,
////                RIGHT_EYE_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
////                imageSize,
////                RIGHT_EYEBROW_COLOR,
////                RIGHT_EYEBROW_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_LEFT_EYE,
////                imageSize,
////                LEFT_EYE_COLOR,
////                LEFT_EYE_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
////                imageSize,
////                LEFT_EYEBROW_COLOR,
////                LEFT_EYEBROW_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_FACE_OVAL,
////                imageSize,
////                FACE_OVAL_COLOR,
////                FACE_OVAL_THICKNESS
////            )
////            drawLandmarksOnCanvas(
////                canvas,
////                result.multiFaceLandmarks()[i].landmarkList,
////                FaceMeshConnections.FACEMESH_LIPS,
////                imageSize,
////                LIPS_COLOR,
////                LIPS_THICKNESS
////            )
////            if (result.multiFaceLandmarks()[i].landmarkCount
////                == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES
////            ) {
////                drawLandmarksOnCanvas(
////                    canvas,
////                    result.multiFaceLandmarks()[i].landmarkList,
////                    FaceMeshConnections.FACEMESH_RIGHT_IRIS,
////                    imageSize,
////                    RIGHT_EYE_COLOR,
////                    RIGHT_EYE_THICKNESS
////                )
////                drawLandmarksOnCanvas(
////                    canvas,
////                    result.multiFaceLandmarks()[i].landmarkList,
////                    FaceMeshConnections.FACEMESH_LEFT_IRIS,
////                    imageSize,
////                    LEFT_EYE_COLOR,
////                    LEFT_EYE_THICKNESS
////                )
////            }
//
////            val connectionPaint = Paint()
////            var col = Color.parseColor("#FF3030")
////            connectionPaint.setColor(col)
////            connectionPaint.setStrokeWidth(2f)
////            canvas.drawLine(
////                foreHead1.x * imageSize.width,
////                foreHead1.y * imageSize.height,
////                foreHead2.x * imageSize.width,
////                foreHead2.y * imageSize.height,
////                connectionPaint
////            )
////            canvas.drawLine(
////                foreHead3.x * imageSize.width,
////                foreHead3.y * imageSize.height,
////                foreHead4.x * imageSize.width,
////                foreHead4.y * imageSize.height,
////                connectionPaint
////            )
            }
        }

    }

    /** Updates the image view with the latest [FaceMeshResult].  */
    fun update() {
        postInvalidate()
        if (latest != null) {
            setImageBitmap(latest)
        }
    }

    fun updateBmp(bitmap: Bitmap) {
        if (bitmap != null) {
            setImageBitmap(bitmap)
        }
    }

    private fun drawLandmarksOnCanvas(
        canvas: Canvas,
        faceLandmarkList: List<NormalizedLandmark>,
        connections: ImmutableSet<FaceMeshConnections.Connection>,
        imageSize: Size,
        color: Int,
        thickness: Int
    ) {
        // Draw connections.
        for (c in connections) {
            val connectionPaint = Paint()
            connectionPaint.setColor(color)
            connectionPaint.setStrokeWidth(thickness.toFloat())
            val start = faceLandmarkList[c.start()]
            val end = faceLandmarkList[c.end()]
            canvas.drawLine(
                start.x * imageSize.getWidth(),
                start.y * imageSize.getHeight(),
                end.x * imageSize.getWidth(),
                end.y * imageSize.getHeight(),
                connectionPaint
            )
        }
    }

    private fun drawLandmarks(
        canvas: Canvas,
        faceLandmarkList: List<NormalizedLandmark>,
        connections: Array<Int>,
        imageSize: Size,
        color: Int,
        thickness: Int
    ) {
        // Draw connections.
//        for (c in connections) {
//            val connectionPaint = Paint()
//            connectionPaint.setColor(color)
//            connectionPaint.setStrokeWidth(thickness.toFloat())
//            val start = faceLandmarkList[c[0]]
//            val end = faceLandmarkList[c.end()]
//            canvas.drawLine(
//                start.x * imageSize.getWidth(),
//                start.y * imageSize.getHeight(),
//                end.x * imageSize.getWidth(),
//                end.y * imageSize.getHeight(),
//                connectionPaint
//            )
//        }


        var i = 0
        while (i < (connections.size - 1)) {
            val connectionPaint = Paint()
            connectionPaint.setColor(color)
            connectionPaint.setStrokeWidth(thickness.toFloat())
            val start = faceLandmarkList[connections[i]]
            val end = faceLandmarkList[connections[i + 1]]
            i++
            canvas.drawLine(
                start.x * imageSize.getWidth(),
                start.y * imageSize.getHeight(),
                end.x * imageSize.getWidth(),
                end.y * imageSize.getHeight(),
                connectionPaint
            )

//            if (i == connections.size) {
//                val start = faceLandmarkList[connections[i]]
//                val end = faceLandmarkList[connections[i+1 - connections.size]]
//                i++
//                canvas.drawLine(
//                    start.x * imageSize.getWidth(),
//                    start.y * imageSize.getHeight(),
//                    end.x * imageSize.getWidth(),
//                    end.y * imageSize.getHeight(),
//                    connectionPaint
//                )
//            }

        }

    }

    companion object {
        private const val TAG = "FaceMeshResultImageView"
        private val TESSELATION_COLOR = Color.parseColor("#70C0C0C0")
        private const val TESSELATION_THICKNESS = 3 // Pixels
        private val RIGHT_EYE_COLOR = Color.parseColor("#FF3030")
        private const val RIGHT_EYE_THICKNESS = 5 // Pixels
        private val RIGHT_EYEBROW_COLOR = Color.parseColor("#FF3030")
        private const val RIGHT_EYEBROW_THICKNESS = 5 // Pixels
        private val LEFT_EYE_COLOR = Color.parseColor("#30FF30")
        private const val LEFT_EYE_THICKNESS = 5 // Pixels
        private val LEFT_EYEBROW_COLOR = Color.parseColor("#30FF30")
        private const val LEFT_EYEBROW_THICKNESS = 5 // Pixels
        private val FACE_OVAL_COLOR = Color.parseColor("#E0E0E0")
        private const val FACE_OVAL_THICKNESS = 5 // Pixels
        private val LIPS_COLOR = Color.parseColor("#E0E0E0")
        private const val LIPS_THICKNESS = 5 // Pixels
    }
}