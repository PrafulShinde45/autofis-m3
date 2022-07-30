package me.siddheshkothadi.autofism3.ui.screens

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.Image
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import me.siddheshkothadi.autofism3.CustomImageAnalyzer
import timber.log.Timber

@Composable
fun CameraScreen(
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val h = remember { mutableStateOf(0f) }
    val w = remember { mutableStateOf(0f) }

    val top = remember { mutableStateOf(0f) }
    val bottom = remember { mutableStateOf(0f) }
    val left = remember { mutableStateOf(0f) }
    val right = remember { mutableStateOf(0f) }

    val detectedLabel = remember { mutableStateOf("") }
    val accuracyText = remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.statusBars.only(
                    WindowInsetsSides.Top
                )
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Box() {
            Box {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val executor = ContextCompat.getMainExecutor(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .build()
                                .apply {
                                    setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build()

                            imageCapture = ImageCapture.Builder()
                                .setTargetRotation(previewView.display.rotation)
                                .build()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .apply {
                                    setAnalyzer(
                                        executor,
                                        CustomImageAnalyzer { rect, label, imageHeight, imageWidth ->
                                            top.value = rect.top * 1f
                                            bottom.value = rect.bottom * 1f
                                            left.value = rect.left * 1f
                                            right.value = rect.right * 1f

                                            h.value = imageHeight * 1f
                                            w.value = imageWidth * 1f

                                            detectedLabel.value = label.text
                                            accuracyText.value = label.confidence.toString()
                                        })
                                }

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                imageAnalysis,
                                imageCapture,
                                preview
                            )
                        }, executor)
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleY = size.height * 1f / w.value
                    val scaleX = size.width * 1f / h.value
                    drawContext.canvas.nativeCanvas.apply {
                        drawRect(
                            RectF(
                                left.value * scaleX,
                                top.value * scaleY,
                                right.value * scaleX,
                                bottom.value * scaleY
                            ),
                            Paint().apply {
                                color = Color.WHITE
                                strokeWidth = 8F
                                style = Paint.Style.STROKE
                            }
                        )
                        drawText(
                            detectedLabel.value,
                            left.value * scaleX + 256f,
                            top.value * scaleY - 80f,
                            Paint().apply {
                                textSize = 48f
                                color = Color.WHITE
                                textAlign = Paint.Align.CENTER
                            }
                        )
                        drawText(
                            accuracyText.value,
                            left.value * scaleX + 256f,
                            top.value * scaleY - 20f,
                            Paint().apply {
                                textSize = 50f
                                color = Color.WHITE
                                textAlign = Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (camera != null) {
                        if (isFlashOn) {
                            if (camera!!.cameraInfo.hasFlashUnit()) {
                                camera!!.cameraControl.enableTorch(false)
                                isFlashOn = false
                            }
                        } else {
                            if (camera!!.cameraInfo.hasFlashUnit()) {
                                camera!!.cameraControl.enableTorch(true)
                                isFlashOn = true
                            }
                        }
                    }
                }) {
                    if (isFlashOn) Icon(
                        Icons.Filled.FlashOn,
                        "", tint = androidx.compose.ui.graphics.Color.White
                    ) else Icon(
                        Icons.Filled.FlashOff,
                        "",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                Text(
                    "AutoFIS",
                    style = MaterialTheme.typography.titleLarge,
                    color = androidx.compose.ui.graphics.Color.White
                )
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(Icons.Filled.MoreVert, "", tint = androidx.compose.ui.graphics.Color.White)
                }
            }

            ElevatedButton(
                modifier = Modifier
                    .padding(12.dp)
                    .size(64.dp)
                    .align(Alignment.BottomCenter),
                onClick = {
                    imageCapture?.takePicture(
                        ContextCompat.getMainExecutor(context), // Defines where the callbacks are run
                        object : ImageCapture.OnImageCapturedCallback() {
                            @ExperimentalGetImage
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                val image: Image? =
                                    imageProxy.image // Do what you want with the image
                                imageProxy.close() // Make sure to close the image
                                Timber.i("Image captured $image")
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Timber.tag("Image Capture").e(exception.toString())
                            }
                        }
                    )
                },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Filled.CameraAlt, "Camera")
            }
        }
    }
}