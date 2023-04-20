package com.example.facemeshrecognition
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Context
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.hardware.camera2.CameraMetadata
//import android.hardware.camera2.CaptureRequest
//import android.os.Build
//import android.util.Log
//import android.util.Range
//import android.view.View
//import android.widget.FrameLayout
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.camera2.interop.Camera2CameraControl
//import androidx.camera.camera2.interop.CaptureRequestOptions
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ProcessLifecycleOwner
//import com.google.common.util.concurrent.ListenableFuture
//import com.google.mediapipe.formats.proto.LandmarkProto
//import com.google.mediapipe.solutions.facemesh.FaceMesh
//import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
//import com.google.mediapipe.solutions.facemesh.FaceMeshResult
//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.Channel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.receiveAsFlow
//import java.nio.ByteBuffer
//import java.security.AccessController.getContext
//import java.util.concurrent.Executors
//import javax.inject.Singleton
//
//@Singleton
//class CameraStream(
//    var applicationContext: Context,
//) {
//
//    private lateinit var frameLayout: FrameLayout
//    private var facemesh: FaceMesh? = null
//    private var imageView: FaceMeshResultImageView? = null
//
//    private lateinit var processCameraProviderFuture: ListenableFuture<ProcessCameraProvider>
//    private lateinit var processCameraProvider: ProcessCameraProvider
//    private lateinit var emotion: TextView
////    private lateinit var frameLayout: FrameLayout
//
//    private val cameraOut = Channel<ByteBuffer>(Channel.BUFFERED)
//    private val out: Flow<ByteBuffer> = cameraOut.receiveAsFlow()
//    private var outStreamJob: Job? = null
//    private val executor = Executors.newSingleThreadExecutor()
//
//    private val imageAnalysisBuilder = ImageAnalysis.Builder()
//        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//
//    private var frameCounter = 0
//    private var lastFpsTimestamp = System.currentTimeMillis()
//
//    private lateinit var activity: MainActivity
//
//
//    fun initialize(frameLayout: FrameLayout) {
////        activity = getContext() as MainActivity
//        if (allPermissionsGranted()) {
//
//            CoroutineScope(Dispatchers.Main).launch {
//                init()
//            }
//        } else {
////            ActivityCompat.requestPermissions(
////                activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
////            )
//        }
//
//        this@CameraStream.frameLayout = frameLayout
//        imageView = FaceMeshResultImageView(applicationContext)
//
//        CoroutineScope(Dispatchers.Default).launch {
//
//            outStreamJob?.cancel()
//            outStreamJob = out.onEach { byteBuffer ->
//                val imageBytes = ByteArray(byteBuffer.remaining())
//                byteBuffer.get(imageBytes)
//                val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//
//                withContext(Dispatchers.Main) {
//                    setUpStreaming(bmp)
//                }
//            }.launchIn(CoroutineScope(Dispatchers.Default))
//        }
//
////        Thread(Runnable { kotlin.run {
////            Thread.sleep(3000)
////        } })
////        stopCurrentPipeline()
//    }
//
//    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
//    private fun init() {
//        processCameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
//        processCameraProvider = processCameraProviderFuture.get()
//        setupCamera()
//
//        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
//        facemesh = FaceMesh(
//            applicationContext,
//            FaceMeshOptions.builder()
//                .setStaticImageMode(false)
//                .setRefineLandmarks(true)
//                .setRunOnGpu(true)
//                .build()
//        )
//
//        facemesh!!.setResultListener { faceMeshResult: FaceMeshResult ->
//            logNoseLandmark(faceMeshResult,  /*showPixelValues=*/false)
//
//            try {
//                imageView?.setFaceMeshResult(faceMeshResult);
////                runOnUiThread(Runnable {
////                    kotlin.run { imageView?.update() }
////                })
//                imageView?.post(java.lang.Runnable {
//                    kotlin.run {
//                        imageView?.update()
//                    }
//                })
//            } catch (e: Exception) {
//                Log.d(TAG, "Face detection error : $e")
//            }
//        }
//        frameLayout.removeAllViewsInLayout()
//        imageView!!.setImageDrawable(null);
//        frameLayout.addView(imageView);
//        imageView!!.visibility = View.VISIBLE;
//        imageView!!.rotation = 90F
//        imageView!!.scaleX = -1F
//
//    }
//
//
//    @SuppressLint("UnsafeOptInUsageError")
//    private fun setupCamera() {
//        val imageAnalysis = imageAnalysisBuilder!!.build()
//        processCameraProvider.unbindAll()
//        val camera = processCameraProvider.bindToLifecycle(
//            ProcessLifecycleOwner.get(),
//            CameraSelector.DEFAULT_BACK_CAMERA,
//            imageAnalysis
//        )
//
//        val cameraControl = camera.cameraControl
//        val camera2CameraControl = Camera2CameraControl.from(cameraControl)
//
//        val captureRequestOptions = CaptureRequestOptions.Builder()
//            .setCaptureRequestOption(
//                CaptureRequest.CONTROL_AF_MODE,
//                CameraMetadata.CONTROL_AF_MODE_OFF
//            )
//            .setCaptureRequestOption(
//                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
//                Range.create(60, 60)
//            )
//            .build()
//        camera2CameraControl.captureRequestOptions = captureRequestOptions
//        CoroutineScope(Dispatchers.IO).launch {
//            cameraPreviewCallBack(imageAnalysis)
//        }
//    }
//
//    private fun cameraPreviewCallBack(imageAnalysis: ImageAnalysis) {
//        processCameraProviderFuture.addListener(Runnable {
//            val frameCount = 30
//            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->
//
//                image.use {
//                    // Compute the FPS of the entire pipeline
//                    if (++frameCounter % frameCount == 0) {
//                        frameCounter = 0
//                        val now = System.currentTimeMillis()
//                        val delta = now - lastFpsTimestamp
//                        val fps = 1000 * frameCount.toFloat() / delta
//                        Log.d(TAG, "FPS: ${"%.02f".format(fps)}")
//                        lastFpsTimestamp = now
//                    }
//                    try {
//                        val img = image.toJpeg()
//                        CoroutineScope(Dispatchers.IO).launch {
//                            cameraOut.send(img ?: throw Throwable("Couldn't get JPEG image"))
//                        }
//
//                    } catch (t: Throwable) {
//                        Log.e(TAG, "Error in getting Img : ${t.message}")
//                    }
//                }
//            })
//        }, executor)
//    }
//
//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(
//            applicationContext, it
//        ) == PackageManager.PERMISSION_GRANTED
//    }
//
////    override fun onRequestPermissionsResult(
////        requestCode: Int, permissions: Array<String>, grantResults:
////        IntArray
////    ) {
////        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
////        if (requestCode == REQUEST_CODE_PERMISSIONS) {
////            if (allPermissionsGranted()) {
////                init()
////            } else {
////                Toast.makeText(
////                    applicationContext,
////                    "Permissions not granted by the user.",
////                    Toast.LENGTH_SHORT
////                ).show()
////                activity.finish()
////            }
////        }
////    }
//
//    companion object {
//        private const val TAG = "FaceMeshApp"
//        private const val REQUEST_CODE_PERMISSIONS = 10
//        private val REQUIRED_PERMISSIONS =
//            mutableListOf(
//                Manifest.permission.CAMERA
//            ).apply {
//                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
//                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                }
//            }.toTypedArray()
//    }
//
//    private fun setUpStreaming(bitmap: Bitmap) {
//
//        CoroutineScope(Dispatchers.IO).launch {
//
//            val now = System.currentTimeMillis()
//            facemesh!!.send(bitmap, now)
//
//            //enable fps algorithm at one place only. else it will add the previous fps also and show incorrect fps.
////            if (++frameCounter % frameCount == 0) {
////                frameCounter = 0
////                val now = System.currentTimeMillis()
////                val delta = now - lastFpsTimestamp
////                val fps = 1000 * frameCount.toFloat() / delta
////                Log.d(TAG, "FPS in mediapipe: ${"%.02f".format(fps)}")
////                lastFpsTimestamp = now
////            }
//        }
//
//    }
//
//    private fun logNoseLandmark(result: FaceMeshResult?, showPixelValues: Boolean) {
//        if (result == null || result.multiFaceLandmarks().isEmpty()) {
//            return
//        }
//        val noseLandmark: LandmarkProto.NormalizedLandmark =
//            result.multiFaceLandmarks()[0].landmarkList[477] //total 477 points
//        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
//        if (showPixelValues) {
//            val width = result.inputBitmap().width
//            val height = result.inputBitmap().height
//            Log.i(
//                TAG,
//                java.lang.String.format(
//                    "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
//                    noseLandmark.getX() * width, noseLandmark.getY() * height
//                )
//            )
//        } else {
//            Log.i(
//                TAG,
//                java.lang.String.format(
//                    "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
//                    noseLandmark.getX(), noseLandmark.getY()
//                )
//            )
//        }
//    }
//
//    //call this method after 60seconds of scanning.Expose this method to sdk
//    private fun stopCurrentPipeline() {
//
//        if (facemesh != null) {
//            facemesh!!.close()
//        }
//        frameLayout.visibility = View.GONE
//    }
//}