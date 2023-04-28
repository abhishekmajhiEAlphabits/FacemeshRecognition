package com.example.facemeshrecognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import java.lang.Runnable
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var rgbBytes: IntArray? = null
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var yRowStride = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private val previewWidth = 1920
    private val previewHeight = 1080
    private var cameraFPS: Float? = null;

    val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
    private var downloadThreadPool: ThreadPoolExecutor? = null
    private var downaloadWorkQueue: LinkedBlockingQueue<Runnable>? = null

    private val CORE_POOL_SIZE = NUMBER_OF_CORES * 100
    private val MAX_POOL_SIZE = NUMBER_OF_CORES * 100
    private val KEEP_ALIVE_TIME = 60L

    val threadExecutor = Executors.newFixedThreadPool(100)
    protected val dispatcher = provideDispatcher(nThreads = 10)
    private var facemesh: FaceMesh? = null
    private var imageView: FaceMeshResultImageView? = null
    private lateinit var cameraView: ImageView

    private lateinit var processCameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var emotion: TextView
    private lateinit var frameLayout: FrameLayout
    private lateinit var btnStartScan: Button

//    private val cameraOut = Channel<ByteBuffer>(Channel.BUFFERED)
//    private val out: Flow<ByteBuffer> = cameraOut.receiveAsFlow()

    private val cameraOut = Channel<ByteArray>(Channel.BUFFERED)
    private val out: Flow<ByteArray> = cameraOut.receiveAsFlow()
    private var outStreamJob: Job? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val imageAnalysisBuilder = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetResolution(Size(1080, 1920))
//        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)

    var frameCounter = 0
    var toastCounter = 0
    var lastFpsTimestamp = System.currentTimeMillis()
    var array: ByteArray? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rgbBytes = IntArray(previewWidth * previewHeight)
        imageView = FaceMeshResultImageView(applicationContext)
        frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        cameraView = findViewById(R.id.cameraImage)

//        btnStartScan = findViewById(R.id.startScanBtn)
//        camera = CameraStream(applicationContext)

        emotion = findViewById(R.id.mEmotion)

        if (allPermissionsGranted()) {

            CoroutineScope(Dispatchers.Main).launch {
                init()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        CoroutineScope(Dispatchers.Default).launch {

            outStreamJob?.cancel()
            outStreamJob = out.onEach { byteBuffer ->

//                val imageBytes = ByteArray(byteBuffer!!.remaining())
//                byteBuffer!!.get(imageBytes)
//                val bmp = BitmapFactory.decodeByteArray(byteBuffer, 0, byteBuffer.size)

//                val bmp = imageBytes.image
//                Log.d(TAG,"byte : $imageBytes")
//                setUpStreaming(bmp)
//                val bmp = byteBuffer.decodeToBitMap()
//                val worker = Runnable {
//                    val bmp = byteBuffer.decodeToBitMap()
////                    setUpStreaming(bmp!!)
//                    CoroutineScope(Dispatchers.Default).launch {
////                        cameraView.setImageBitmap(bmp)
//
//                    }
//                }
//                threadExecutor.execute(worker)
//                threadExecutor.shutdown()
//                executor.awaitTermination(1, TimeUnit.HOURS)
                withContext(Dispatchers.Main) {

//                    val bmp = byteBuffer.decodeToBitMap(byteBuffer)
//                        if (bmp != null) {
////                        setUpStreaming(bmp!!)
//
////                        cameraView.rotation = -90F
//                        }
                }
            }.launchIn(lifecycleScope)

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun init() {
        downaloadWorkQueue = LinkedBlockingQueue<Runnable>()

        downloadThreadPool = ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE,
            KEEP_ALIVE_TIME, TimeUnit.SECONDS, downaloadWorkQueue
        )

        processCameraProviderFuture = ProcessCameraProvider.getInstance(this)
        processCameraProvider = processCameraProviderFuture.get()
        setupCamera()


        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh = FaceMesh(
            applicationContext,
            FaceMeshOptions.builder()
                .setStaticImageMode(false)
                .setRefineLandmarks(true)
                .setRunOnGpu(true)
                .build()
        )

        facemesh!!.setResultListener { faceMeshResult: FaceMeshResult ->
            logNoseLandmark(faceMeshResult,  /*showPixelValues=*/false)

            try {
                imageView?.setFaceMeshResult(faceMeshResult);
                runOnUiThread(Runnable {
                    kotlin.run { imageView?.update() }
                })
            } catch (e: Exception) {
                Log.d(TAG, "Face detection error : $e")
            }
        }
        frameLayout.removeAllViewsInLayout()
        imageView!!.setImageDrawable(null);
        frameLayout.addView(imageView);
        imageView!!.visibility = View.VISIBLE;
        imageView!!.rotation = 90F
        imageView!!.scaleX = -1F


    }

    fun startScanning(view: View) {
        start()
    }

    fun start() {
//        camera.initialize(frameLayout)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeOptInUsageError")
    private fun setupCamera() {
        val imageAnalysis = imageAnalysisBuilder!!.build()
        processCameraProvider.unbindAll()
        val camera = processCameraProvider.bindToLifecycle(
            this,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            imageAnalysis
        )

        val params = camera.cameraInfo
//        val cameraParams: Camera.Parameters = camera.cameraInfo.
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
//        var size = getResolution(params, Quality.LOWEST);


        val cameraCharacteristics = cameraManager.getCameraCharacteristics("1")
        val streamConfigurationMap: StreamConfigurationMap? =
            cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes: Array<Size> = streamConfigurationMap!!.getOutputSizes(
            SurfaceTexture::class.java
        )
        for (i in 0 until sizes.size) {
            var result = sizes[i]
            Log.i(
                "abhi",
                "Supported Size. Width: " + result.width.toString() + " height : " + result.height
            )
        }
//        Log.d("abhi", "$sizes")


        val cameraControl = camera.cameraControl
        val camera2CameraControl = Camera2CameraControl.from(cameraControl)

        val captureRequestOptions = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_OFF
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range.create(60, 60)
            )

//            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
//            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
//            .setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            .build()
        camera2CameraControl.captureRequestOptions = captureRequestOptions
        CoroutineScope(Dispatchers.IO).launch {
            cameraPreviewCallBack(imageAnalysis)
            imageAnalysis.clearAnalyzer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeOptInUsageError")
    private fun cameraPreviewCallBack(imageAnalysis: ImageAnalysis) {
        processCameraProviderFuture.addListener(Runnable {
            val frameCount = 30
            imageAnalysis.setAnalyzer(executor, ImageAnalysis.Analyzer { image ->

                image.use {
                    // Compute the FPS of the entire pipeline
                    if (++frameCounter % frameCount == 0) {
                        frameCounter = 0
                        val now = System.currentTimeMillis()
                        val delta = now - lastFpsTimestamp
                        val fps = 1000 * frameCount.toFloat() / delta
                        Log.d(TAG, "FPS: ${"%.02f".format(fps)}")
                        cameraFPS = fps
                        lastFpsTimestamp = now
                    }
                    if (++toastCounter % 200 == 0) {
                        toastCounter = 0
                        runOnUiThread(Runnable {
                            kotlin.run {
                                Toast.makeText(
                                    applicationContext,
                                    "FPS : $cameraFPS",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                    }

                    try {
//                        val img = image.toNv21(image.image!!)
                        val img = Util.YUV420toNV21(image.image)

//                        try {
//                            val planes = image.image!!.planes
//                            fillBytes(planes, yuvBytes)
//                            yRowStride = planes[0].rowStride
//                            val uvRowStride = planes[1].rowStride
//                            val uvPixelStride = planes[1].pixelStride
////                            imageConverter = Runnable {
//                                image.convertYUV420ToARGB8888(
//                                    yuvBytes[0]!!,
//                                    yuvBytes[1]!!,
//                                    yuvBytes[2]!!,
//                                    previewWidth,
//                                    previewHeight,
//                                    yRowStride,
//                                    uvRowStride,
//                                    uvPixelStride,
//                                    rgbBytes!!
//                                )
////                            }
////                            postInferenceCallback = Runnable {
//////                                image.close()
//////                                isProcessingFrame = false
////                            }
////                            rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
////                            rgbFrameBitmap?.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight)
//                            CoroutineScope(Dispatchers.Main).launch {
////                                cameraView.setImageBitmap(rgbFrameBitmap)
//                            }
//                        } catch (e: Exception) {
//                            Log.d(TAG, "$e")
//                        }

                        val worker = Runnable {
                            val bmp = img.decodeToBitMap()
                            CoroutineScope(Dispatchers.Main).launch {
                                cameraView.setImageBitmap(bmp)
//                                setUpStreaming(bmp!!)
                            }
                        }
                        downloadThreadPool!!.execute(worker)
                        CoroutineScope(Dispatchers.IO).launch {
//                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                            cameraView.setImageBitmap(bmp)
//                            cameraOut.send(
//                                img ?: throw Throwable("Couldn't get JPEG image")
//                            )
                        }
//                        }

                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in getting Img : ${t.message}")
                    }

                }
//                image.close()
            })
        }, executor)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                init()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "FaceMeshApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun setUpStreaming(bitmap: Bitmap) {

        CoroutineScope(Dispatchers.Main).launch {
//
//            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val now = System.currentTimeMillis()
//            val tsLong = System.currentTimeMillis() * 10
            facemesh!!.send(bitmap, now)

            //enable fps algorithm at one place only. else it will add the previous fps also and show incorrect fps.
//            if (++frameCounter % frameCount == 0) {
//                frameCounter = 0
//                val now = System.currentTimeMillis()
//                val delta = now - lastFpsTimestamp
//                val fps = 1000 * frameCount.toFloat() / delta
//                Log.d(TAG, "FPS in mediapipe: ${"%.02f".format(fps)}")
//                lastFpsTimestamp = now
//            }
        }

    }

    //
    private fun logNoseLandmark(result: FaceMeshResult?, showPixelValues: Boolean) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return
        }
        val noseLandmark: LandmarkProto.NormalizedLandmark =
            result.multiFaceLandmarks()[0].landmarkList[477] //total 477 points
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            Log.i(
                TAG,
                java.lang.String.format(
                    "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
                    noseLandmark.getX() * width, noseLandmark.getY() * height
                )
            )
        } else {
            Log.i(
                TAG,
                java.lang.String.format(
                    "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    noseLandmark.getX(), noseLandmark.getY()
                )
            )
        }
    }

    //call this method after 60seconds of scanning.Expose this method to sdk
    private fun stopCurrentPipeline() {

//        if (facemesh != null) {
//            facemesh!!.close()
//        }
    }

    protected fun fillBytes(
        planes: Array<Image.Plane>,
        yuvBytes: Array<ByteArray?>
    ) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    private fun processImage(): Bitmap? {
        imageConverter!!.run()
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        rgbFrameBitmap?.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight)
        return rgbFrameBitmap
//        postInferenceCallback!!.run()
    }

}