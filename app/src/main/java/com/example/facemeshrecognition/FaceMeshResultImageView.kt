package com.example.facemeshrecognition

import android.content.Context
import android.graphics.*
import android.util.Size
import androidx.appcompat.widget.AppCompatImageView
import com.google.common.collect.ImmutableSet
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import javax.inject.Singleton

@Singleton
class FaceMeshResultImageView(context: Context?) : AppCompatImageView(context) {
    private lateinit var latest: Bitmap

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
//        val noseLandmark: LandmarkProto.NormalizedLandmark =
//            result.multiFaceLandmarks()[0].landmarkList[472]
//        val noseLandmarks: LandmarkProto.NormalizedLandmark =
//            result.multiFaceLandmarks()[0].landmarkList[477]
        val numFaces = result.multiFaceLandmarks().size
        for (i in 0 until numFaces) {
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_TESSELATION,
                imageSize,
                TESSELATION_COLOR,
                TESSELATION_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_RIGHT_EYE,
                imageSize,
                RIGHT_EYE_COLOR,
                RIGHT_EYE_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
                imageSize,
                RIGHT_EYEBROW_COLOR,
                RIGHT_EYEBROW_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LEFT_EYE,
                imageSize,
                LEFT_EYE_COLOR,
                LEFT_EYE_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
                imageSize,
                LEFT_EYEBROW_COLOR,
                LEFT_EYEBROW_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_FACE_OVAL,
                imageSize,
                FACE_OVAL_COLOR,
                FACE_OVAL_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LIPS,
                imageSize,
                LIPS_COLOR,
                LIPS_THICKNESS
            )
            if (result.multiFaceLandmarks()[i].landmarkCount
                == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES
            ) {
                drawLandmarksOnCanvas(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    FaceMeshConnections.FACEMESH_RIGHT_IRIS,
                    imageSize,
                    RIGHT_EYE_COLOR,
                    RIGHT_EYE_THICKNESS
                )
                drawLandmarksOnCanvas(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    FaceMeshConnections.FACEMESH_LEFT_IRIS,
                    imageSize,
                    LEFT_EYE_COLOR,
                    LEFT_EYE_THICKNESS
                )
            }

//            val connectionPaint = Paint()
//            var col = Color.parseColor("#FF3030")
//            connectionPaint.setColor(col)
//            connectionPaint.setStrokeWidth(2f)
//            canvas.drawLine(
//                noseLandmark.x * imageSize.width,
//                noseLandmark.y * imageSize.height,
//                noseLandmarks.x * imageSize.width,
//                noseLandmarks.y * imageSize.height,
//                connectionPaint
//            )
        }
    }

    /** Updates the image view with the latest [FaceMeshResult].  */
    fun update() {
        postInvalidate()
        if (latest != null) {
            setImageBitmap(latest)
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