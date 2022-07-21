package ru.mrapple100.pointer2point.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ru.`object`.detection.camera.DebugHelper
import ru.mrapple100.pointer2point.detection.DetectionResult
import ru.`object`.detection.detection.ObjectDetector
import ru.`object`.detection.util.ImageUtil
import ru.`object`.detection.util.YuvToRgbConverter
import java.util.concurrent.atomic.AtomicInteger

class ObjectDetectorAnalyzer(
        private val context: Context,
        private val config: Config,
        private val onDetectionResult: (Result) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "ObjectDetectorAnalyzer"
        private val DEBUG = false
    }

    private val iterationCounter = AtomicInteger(0)

    private val debugHelper = DebugHelper(
            saveResult = false,
            context = context,
            resultHeight = config.inputSize,
            resultWidth = config.inputSize
    )

    private val yuvToRgbConverter = YuvToRgbConverter(context)

    private val uiHandler = Handler(Looper.getMainLooper())

    private var inputArray = IntArray(config.inputSize * config.inputSize)

    private var objectDetector: ObjectDetector? = null

    private var rgbBitmap: Bitmap? = null
    private var resizedBitmap = Bitmap.createBitmap(config.inputSize, config.inputSize, Bitmap.Config.ARGB_8888)

    private var matrixToInput: Matrix? = null

    override fun analyze(image: ImageProxy) {
        val rotationDegrees = image.imageInfo.rotationDegrees

        val iteration = iterationCounter.getAndIncrement()

        val rgbBitmap = getArgbBitmap(image.width, image.height)

        yuvToRgbConverter.yuvToRgb(image, rgbBitmap)

        val transformation = getTransformation(rotationDegrees, image.width, image.height)

        image.close()

        Canvas(resizedBitmap).drawBitmap(rgbBitmap, transformation, null)

        ImageUtil.storePixels(resizedBitmap, inputArray)
        //inputArray - массив 300 на 300 где есть цвета
        //-16777216 - black
        //-1 - white
        //-6028544 - red
        val colorInt = inputArray[0]
        Log.d("IMAGE300",colorInt.toString())
        val hex = Integer.toString(16777216 + colorInt, 16)
        Log.d("IMAGE300",hex.toString())

        // Log.d("IMAGE300", hex)
        if (hex.length == 6) {
            /*Log.d(
                "IMAGE300",
                hex2Rgb(hex)!!.components[0].toString() + " " +
                        hex2Rgb(hex)!!.components[1].toString() + " " +
                        hex2Rgb(hex)!!.components[2].toString() + " "
            )*/
        }
        var y1 = 0
        var x1 = 0
        for(i in inputArray.indices) {
            val colorInt = inputArray[i]
            val hex = Integer.toString(16777216 + colorInt, 16)
           // Log.d("IMAGE300", hex)
            if (hex.length == 6) {
                /*Log.d(
                    "IMAGE300",
                    hex2Rgb(hex)!!.components[0].toString() + " " +
                            hex2Rgb(hex)!!.components[1].toString() + " " +
                            hex2Rgb(hex)!!.components[2].toString() + " "
                )*/

                if (hex2Rgb(hex)!!.components[0] > 80 && hex2Rgb(hex)!!.components[0] < 250 && hex2Rgb(hex)!!.components[1] < 90 && hex2Rgb(hex)!!.components[2] > 100
                ) {
                     y1 = i / config.inputSize
                     x1 = i % config.inputSize
                    Log.d("IMAGE300", "ФИОЛЕТОВЫЙ1111")
                    Log.d(
                        "IMAGE300",
                        hex2Rgb(hex)!!.components[0].toString() + " " +
                                hex2Rgb(hex)!!.components[1].toString() + " " +
                                hex2Rgb(hex)!!.components[2].toString() + " "
                    )
                    break

                }
            }
        }
        var y2 = 0
        var x2 = 0
        for(i in inputArray.indices) {
            val colorInt = inputArray[((300*300)-i-1)]
            val hex = Integer.toString(16777216 + colorInt, 16)
            // Log.d("IMAGE300", hex)
            if (hex.length == 6) {
                /*Log.d(
                    "IMAGE300",
                    hex2Rgb(hex)!!.components[0].toString() + " " +
                            hex2Rgb(hex)!!.components[1].toString() + " " +
                            hex2Rgb(hex)!!.components[2].toString() + " "
                )*/

                if (hex2Rgb(hex)!!.components[0] > 80 && hex2Rgb(hex)!!.components[0] < 250 && hex2Rgb(hex)!!.components[1] < 90 && hex2Rgb(hex)!!.components[2] > 100
                ) {
                    y2 = (inputArray.size-i-1) / config.inputSize
                    x2 = (inputArray.size-i-1) % config.inputSize
                    Log.d("IMAGE300", "ФИОЛЕТОВЫЙ2222")
                    Log.d(
                        "IMAGE300",
                        hex2Rgb(hex)!!.components[0].toString() + " " +
                                hex2Rgb(hex)!!.components[1].toString() + " " +
                                hex2Rgb(hex)!!.components[2].toString() + " "
                    )
                    break

                }
            }
        }
        //val objects = detect(inputArray)

//        if (DEBUG) {
//            debugHelper.saveResult(iteration, resizedBitmap, objects)
//        }

       // Log.d(TAG, "detection objects($iteration): $objects")
       val detectionResult = DetectionResult(
           x1 = x1,
           y1 = y1,
           x2 = x2,
           y2 = y2
       )
        val list = mutableListOf<DetectionResult>()
        if(x1!=0 && y1!=0 && x2!=0 &&y2!=0){
            list.add(detectionResult)
        }
        val result = Result(
                pointer = list,
                imageWidth = config.inputSize,
                imageHeight = config.inputSize,
        )



        uiHandler.post {
            onDetectionResult.invoke(result)
        }
    }

    fun hex2Rgb(colorStr: String): Color? {
        return Color.valueOf(
            Integer.valueOf(colorStr.substring(0, 2), 16).toFloat()!!,
            Integer.valueOf(colorStr.substring(2, 4), 16).toFloat()!!,
            Integer.valueOf(colorStr.substring(4, 6), 16).toFloat()!!
        )
    }

    private fun getTransformation(rotationDegrees: Int, srcWidth: Int, srcHeight: Int): Matrix {
        var toInput = matrixToInput
        if (toInput == null) {
            toInput = ImageUtil.getTransformMatrix(rotationDegrees, srcWidth, srcHeight, config.inputSize, config.inputSize)
            matrixToInput = toInput
        }
        return toInput
    }

    private fun getArgbBitmap(width: Int, height: Int): Bitmap {
        var bitmap = rgbBitmap
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) as Bitmap
            rgbBitmap = bitmap
        }
        return bitmap
    }

    /*private fun detect(inputArray: IntArray): List<DetectionResult> {
        var detector = objectDetector
        if (detector == null) {
            detector = ObjectDetector(
                    isModelQuantized = config.isQuantized,
                    inputSize = config.inputSize
            )
            objectDetector = detector
        }

        return detector.detect(inputArray)
    }*/

    data class Config(
            val inputSize: Int,
            val isQuantized: Boolean,
    )

    data class Result(
        val pointer: List< DetectionResult>,
        val imageWidth: Int,
        val imageHeight: Int,
           // val imageRotationDegrees: Int
    )
}