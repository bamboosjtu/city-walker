package com.citywalker.mvp.core.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

data class CameraExposureInfo(
    val indexRange: IntRange,
    val step: Float,
    val currentIndex: Int
)

class CameraCapture(private val context: Context) {
    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null

    fun bind(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        imageCapture: ImageCapture,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            runCatching {
                provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                provider?.unbindAll()
                camera = provider?.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onReady()
            }.onFailure(onError)
        }, ContextCompat.getMainExecutor(context))
    }

    fun capture(
        imageCapture: ImageCapture,
        file: File,
        executor: Executor,
        onSaved: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            options,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSaved()
                override fun onError(exception: ImageCaptureException) = onError(exception)
            }
        )
    }

    fun focusAndMetering(previewView: PreviewView, x: Float, y: Float) {
        val currentCamera = camera ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        currentCamera.cameraControl.startFocusAndMetering(action)
    }

    fun exposureCompensationInfo(): CameraExposureInfo? {
        val exposureState = camera?.cameraInfo?.exposureState ?: return null
        if (!exposureState.isExposureCompensationSupported) return null
        val range = exposureState.exposureCompensationRange
        val step = exposureState.exposureCompensationStep.toFloat()
        if (step <= 0f || range.lower >= range.upper) return null
        return CameraExposureInfo(
            indexRange = range.lower..range.upper,
            step = step,
            currentIndex = exposureState.exposureCompensationIndex
        )
    }

    fun setExposureCompensation(index: Int) {
        val currentCamera = camera ?: return
        val range = currentCamera.cameraInfo.exposureState.exposureCompensationRange
        currentCamera.cameraControl.setExposureCompensationIndex(index.coerceIn(range.lower, range.upper))
    }

    fun unbind() {
        provider?.unbindAll()
        provider = null
        camera = null
    }
}
