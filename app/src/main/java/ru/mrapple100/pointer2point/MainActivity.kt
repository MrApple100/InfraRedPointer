package ru.mrapple100.pointer2point

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import ru.`object`.detection.camera.CameraPermissionsResolver
import ru.mrapple100.pointer2point.camera.ObjectDetectorAnalyzer
import ru.mrapple100.pointer2point.view.ReconView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView

    private lateinit var executor: ExecutorService

    private val cameraPermissionsResolver = CameraPermissionsResolver(this)

    private val objectDetectorConfig = ObjectDetectorAnalyzer.Config(
        inputSize = 300,
        isQuantized = true,
    )

    var result_overlay: ReconView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        executor = Executors.newSingleThreadExecutor()


        result_overlay = findViewById(R.id.result_overlay)

       // result_overlay.setDescriptionText(findViewById(R.id.DescriptionText))


        previewView = findViewById(R.id.preview_view)

        cameraPermissionsResolver.checkAndRequestPermissionsIfNeeded(
            onSuccess = {
                getProcessCameraProvider(::bindCamera)
            },
            onFail = ::showSnackbar
        )
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            executor,
            ObjectDetectorAnalyzer(applicationContext, objectDetectorConfig, ::onDetectionResult)
        )

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            this,
            cameraSelector,
            imageAnalysis,
            preview
        )

        preview.setSurfaceProvider(previewView.createSurfaceProvider())
    }

    private fun getProcessCameraProvider(onDone: (ProcessCameraProvider) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable { onDone.invoke(cameraProviderFuture.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }

   private fun onDetectionResult(result: ObjectDetectorAnalyzer.Result) {
       result_overlay?.updatePointer(result)
    }

    private fun showSnackbar(message: String) {
       // Snackbar.make(root_container, message, Snackbar.LENGTH_LONG).show()
    }


}