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
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.components.TextureFrameConsumer
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.framework.TextureFrame
import com.google.mediapipe.solutioncore.CameraInput
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


class MainActivity : AppCompatActivity() {

//    private lateinit var planesObj: Planes
//    private var cameraInput: CameraInput? = null
//
//    private lateinit var planeY: PlaneY
//    private lateinit var planeU: PlaneU
//    private lateinit var planeV: PlaneV
//    private var iWidth: Int? = null
//    private var iHeight: Int? = null

    private var facemesh: FaceMesh? = null
    private var imageView: FaceMeshResultImageView? = null
    private lateinit var cameraView: ImageView

    private lateinit var processCameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var emotion: TextView
    private lateinit var frameLayout: FrameLayout
    private lateinit var btnStartScan: Button

    private val cameraOut = Channel<ByteBuffer>(Channel.BUFFERED)
    private val out: Flow<ByteBuffer> = cameraOut.receiveAsFlow()

//    private val cameraOut = Channel<Array<ByteBuffer>>(Channel.BUFFERED)
//    private val out: Flow<Array<ByteBuffer>> = cameraOut.receiveAsFlow()
    private var outStreamJob: Job? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val imageAnalysisBuilder = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//        .setTargetResolution(Size(640,480))

    var frameCounter = 0
    var lastFpsTimestamp = System.currentTimeMillis()
    var array: ByteArray? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

                val imageBytes = ByteArray(byteBuffer!!.remaining())
                byteBuffer!!.get(imageBytes)
                val bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//                val bmp = yuv.decodeToBitMap(yuv)
//                val bmp = imageBytes.image
//                Log.d(TAG,"byte : $imageBytes")
//                setUpStreaming(bmp)
                withContext(Dispatchers.Main) {
                    if (bmp != null) {
                        setUpStreaming(bmp!!)
//                    cameraView.setImageBitmap(bmp)
                    }
                }
            }.launchIn(lifecycleScope)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun init() {
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
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeOptInUsageError")
    private fun cameraPreviewCallBack(imageAnalysis: ImageAnalysis) {
        var imageUtil = Util()
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
                        lastFpsTimestamp = now
                    }

                    try {

//                        val img = image.getByteArray(image)
//                        Log.d("sam", "${image.image!!.width} :: ${image!!.image!!.height}")
//                        val img = image.image
                        val img = image.toJpeg()
//                        val img = image.toYUV()


//                        planeY.buffer = image.image!!.planes[0].buffer;
//                        planeU.buffer = image.image!!.planes[1].buffer;
//                        planeV.buffer = image.image!!.planes[2].buffer;
//                        planeY.rowStride = image.image!!.planes[0].rowStride;
//                        planeU.rowStride = image.image!!.planes[1].rowStride;
//                        planeV.rowStride = image.image!!.planes[2].rowStride;
//                        planeY.pixelStride = image.image!!.planes[0].pixelStride;
//                        planeU.pixelStride = image.image!!.planes[1].pixelStride;
//                        planeV.pixelStride = image.image!!.planes[2].pixelStride;

//                        var y = image.image!!.planes[0].buffer;
//                        var u = image.image!!.planes[1];
//                        var v = image.image!!.planes[2];

//                        val y = image.image!!.planes[0].buffer;
//                        val u = image.image!!.planes[1].buffer;
//                        val v = image.image!!.planes[2].buffer;
//                        var planeListArray = arrayOf(y, u, v)


//                        val buffer = image.planes[0].buffer
//                        val bytes = ByteArray(buffer.remaining())
//                        buffer.get(bytes)
//                        CoroutineScope(Dispatchers.Main).launch {
////                            var bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
//                            cameraView.setImageBitmap(bitmap)
//                        }
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);


//                        var plane : Array<Image.Plane> = image.image!!.planes;
//                        val yuv = image.toYUV()
//                        Log.d("test","Test ${image.width} ${image.height}");


                        /**
                         * Option 2
                         */
//                        var bitmapBuffer : Bitmap = Bitmap.createBitmap(image.width,image.height,Bitmap.Config.ARGB_8888);
//                        bitmapBuffer.copyPixelsFromBuffer(image.image!!.planes[0].buffer);


//                        val img = image.toYuvImage(image.image!!)
//                        val img = image.toByteArray()

//                        var base64value : String = Base64.encodeToString(img, Base64.DEFAULT);

                        CoroutineScope(Dispatchers.IO).launch {
//                            val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//                            Log.d("hardik","Receive data");
//                            val bmp = img.decodeToBitMap()

//                            Log.d(TAG, "bytearray : $img")
                            cameraOut.send(
                                img ?: throw Throwable("Couldn't get JPEG image")
                            )
                        }

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

        CoroutineScope(Dispatchers.IO).launch {
//
//            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val now = System.currentTimeMillis()
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


}