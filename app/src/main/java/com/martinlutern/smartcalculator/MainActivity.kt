package com.martinlutern.smartcalculator

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.martinlutern.smartcalculator.ui.theme.SmartCalculatorTheme
import com.martinlutern.smartcalculator.ui.view.CameraView.getCameraProvider
import com.martinlutern.smartcalculator.ui.view.CameraView.takePhoto
import com.martinlutern.smartcalculator.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartCalculatorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DefaultPreview()
                }
            }
        }

        if (viewModel.isCameraType()) {
            requestCameraPermission()
        }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun recognizeText(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image).addOnSuccessListener { visionText ->
            var finalVisionText: String = visionText.text
            finalVisionText = finalVisionText.toLowerCase().replace("l", "1")
            finalVisionText = finalVisionText.toLowerCase().replace("i", "1")
            finalVisionText = finalVisionText.toLowerCase().replace("o", "0")
            finalVisionText = finalVisionText.toLowerCase().replace("x", "*")
            val regex = Regex("[^A-Za-z ]")
            finalVisionText = regex.replace(finalVisionText, "")
            println("Result: " + finalVisionText)
            viewModel.hideLoading()
            viewModel.setImageCalculation(finalVisionText)
            var result: Int = 0
            when (finalVisionText) {
                "+" -> {
                    val splittedText = finalVisionText.split("+").toTypedArray()
                    var firstNumber = 0
                    var secondNumber = 0
                    if (splittedText.isNotEmpty())
                        firstNumber = splittedText[0].toInt()
                    if (splittedText.size > 1)
                        secondNumber = splittedText[1].toInt()
                    result = firstNumber + secondNumber
                }
                "-" -> {
                    val splittedText = finalVisionText.split("-").toTypedArray()
                    var firstNumber = 0
                    var secondNumber = 0
                    if (splittedText.isNotEmpty())
                        firstNumber = splittedText[0].toInt()
                    if (splittedText.size > 1)
                        secondNumber = splittedText[1].toInt()
                    result = firstNumber - secondNumber
                }
                "/" -> {
                    val splittedText = finalVisionText.split("/").toTypedArray()
                    var firstNumber = 1
                    var secondNumber = 1
                    if (splittedText.isNotEmpty())
                        firstNumber = splittedText[0].toInt()
                    if (splittedText.size > 1)
                        secondNumber = splittedText[1].toInt()
                    result = firstNumber / secondNumber
                }
                "*" -> {
                    val splittedText = finalVisionText.split("*").toTypedArray()
                    var firstNumber = 1
                    var secondNumber = 1
                    if (splittedText.isNotEmpty())
                        firstNumber = splittedText[0].toInt()
                    if (splittedText.size > 1)
                        secondNumber = splittedText[1].toInt()
                    result = firstNumber * secondNumber
                }
            }
            viewModel.setImageResult(result.toString())
        }.addOnFailureListener { e ->
            e.printStackTrace()
            viewModel.hideLoading()
            viewModel.handleError(e)
        }
    }

    private fun handleImageCapture(uri: Uri) {
        viewModel.showLoading()
        viewModel.hideCamera()
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this@MainActivity, uri)
            recognizeText(image)
        } catch (e: IOException) {
            e.printStackTrace()
            viewModel.hideLoading()
            viewModel.handleError(e)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (viewModel.isCameraType()) {
                viewModel.showCamera()
            } else {
                viewModel.getContent?.launch("image/*")
            }
        } else {
            val throwable = Throwable("Permission denied")
            throwable.printStackTrace()
            if (viewModel.isCameraType()) {
                viewModel.hideCamera()
            }
            viewModel.handleError(throwable)
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.showCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                viewModel.hideCamera()
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    class GetContentActivityResult(
        private val launcher: ManagedActivityResultLauncher<String, Uri>,
        val uri: Uri?
    ) {
        fun launch(mimeType: String) {
            launcher.launch(mimeType)
        }
    }

    @Composable
    fun rememberGetContentActivityResult(): GetContentActivityResult {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent(), onResult = {
                viewModel.setImageUri(it)
            })
        return remember(launcher, viewModel.imageUri.value) {
            GetContentActivityResult(launcher, viewModel.imageUri.value)
        }
    }

    private fun requestStoragePermission() {
        when {
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                if (viewModel.getContent !== null)
                    viewModel.getContent?.launch("image/*")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                // do nothing
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    @Composable
    fun Visibility(
        visible: Boolean,
        content: @Composable () -> Unit,
        modifier: Modifier? = Modifier
    ) {
        if (modifier !== null) {
            AnimatedVisibility(visible = visible, modifier = modifier) {
                content()
            }
        } else {
            AnimatedVisibility(visible = visible, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }

    @Composable
    fun CameraView(
        outputDirectory: File,
        executor: Executor,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        if (viewModel.shouldShowCamera.value) {
            // 1
            val lensFacing = CameraSelector.LENS_FACING_BACK
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            val preview = androidx.camera.core.Preview.Builder().build()
            val previewView = remember { PreviewView(context) }
            val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // 2
            LaunchedEffect(lensFacing) {
                val cameraProvider = context.getCameraProvider()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                preview.setSurfaceProvider(previewView.surfaceProvider)
            }

            // 3
            Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
                AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

                IconButton(
                    modifier = Modifier.padding(bottom = 20.dp),
                    onClick = {
                        takePhoto(
                            filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS",
                            imageCapture = imageCapture,
                            outputDirectory = outputDirectory,
                            executor = executor,
                            onImageCaptured = onImageCaptured,
                            onError = onError
                        )
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Lens,
                            contentDescription = "Take picture",
                            tint = White,
                            modifier = Modifier
                                .size(100.dp)
                                .padding(1.dp)
                                .border(1.dp, White, CircleShape)
                        )
                    }
                )
            }
        }
    }

    @Composable
    fun Loading() {
        Visibility(visible = viewModel.shouldShowLoading.value, content = {
            Dialog(
                onDismissRequest = { viewModel.hideLoading() },
                DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Box(
                    contentAlignment = Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(White, shape = RoundedCornerShape(8.dp))
                ) {
                    CircularProgressIndicator()
                }
            }
        })
    }

    @Composable
    fun TextResult() {
        if (!viewModel.shouldShowError.value.message.isNullOrEmpty()) {
            Text(
                text = "Error: " + viewModel.shouldShowError.value.message,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.Red
            )
        } else {
            Text(
                text = "Calculation: " + viewModel.imageCalculation.value.ifEmpty { "-" },
                modifier = Modifier.fillMaxWidth(), fontSize = 18.sp, textAlign = TextAlign.Center
            )
            Text(
                text = "Result: " + viewModel.imageCalculation.value.ifEmpty { "-" },
                modifier = Modifier.fillMaxWidth(), fontSize = 16.sp, textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    fun ResultButton() {
        Visibility(
            visible = !viewModel.shouldShowCamera.value, content = {
                Button(
                    onClick = {
                        if (viewModel.isCameraType()) {
                            if (viewModel.shouldShowCamera.value) {
                                viewModel.hideCamera()
                            } else {
                                viewModel.showCamera()
                                viewModel.resetImageResult()
                                viewModel.resetImageCalculation()
                            }
                        } else {
                            viewModel.setImageUri(null)
                        }
                        viewModel.handleError(Throwable(""))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = White)
                ) {
                    Text(text = "Back")
                }
            }, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }

    @Composable
    fun PickerButton() {
        viewModel.getContent = rememberGetContentActivityResult()
        viewModel.getContent?.uri?.let {
            handleImageCapture(it)
        }

        Visibility(
            visible = true, content = {
                Button(
                    onClick = {
                        requestStoragePermission()
                    }, colors = ButtonDefaults.buttonColors(containerColor = White)
                ) {
                    Text(text = "Pick Photo")
                }
            }, modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (viewModel.imageCalculation.value !== "" || viewModel.shouldShowError.value.message !== "") {
                Column(
                    modifier = Modifier
                        .weight(1f, false)
                ) {
                    TextResult()
                }
                Row(
                    modifier = Modifier
                        .weight(1f, false)
                ) {
                    ResultButton()
                }
            } else {
                if (viewModel.isFilesystemType()) {
                    Row(
                        modifier = Modifier
                            .weight(1f, true)
                    ) {
                        PickerButton()
                    }
                }
            }
        }

        if (viewModel.isCameraType()) {
            CameraView(
                outputDirectory = outputDirectory,
                executor = cameraExecutor,
                onImageCaptured = ::handleImageCapture,
                onError = {
                    it.printStackTrace()
                    viewModel.hideCamera()
                    viewModel.handleError(it)
                }
            )
        }
        Loading()
    }
}