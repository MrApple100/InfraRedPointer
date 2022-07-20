package ru.mrapple100.pointer2point.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import ru.mrapple100.pointer2point.camera.ObjectDetectorAnalyzer
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ReconView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var result: ObjectDetectorAnalyzer.Result? = null

    private var standartlenline = 550f

    private val boxPaint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.STROKE
        //alpha = 220 //from 0(transparent) to 255
        strokeWidth = 5f
    }
    private val PointPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        //alpha = 220 //from 0(transparent) to 255
        strokeWidth = 15f
    }
    fun updatePointer(result: ObjectDetectorAnalyzer.Result) {
        this.result = result
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {


        val result = result ?: return
        val scaleFactorX = measuredWidth / result.imageWidth.toFloat()
        val scaleFactorY = measuredHeight / result.imageHeight.toFloat()

        result.pointer.forEach { obj ->
           val x1 = obj.x1 * scaleFactorX
            val y1 = obj.y1 * scaleFactorY
            val x2 = obj.x2 * scaleFactorX
            val y2 = obj.y2 * scaleFactorY

            canvas.drawLine(x1,y1,x2,y2,boxPaint)
            val lenline = sqrt(((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1)).toDouble())
            Log.d("LENLINE","$lenline")
            val angle = acos((x2-x1)/lenline)
            //Math.acos(lenline/standartlenline)
            if(angle>=0 && angle<=90) {
                val coordpointx = result.imageWidth - result.imageWidth * lenline / standartlenline
                val coordpointy = result.imageHeight - result.imageHeight * lenline / standartlenline

                val maxradius = sqrt((result.imageHeight * scaleFactorY * result.imageHeight * scaleFactorY + result.imageWidth * scaleFactorX * result.imageWidth * scaleFactorX).toDouble())
                val radius = maxradius * lenline / standartlenline
                canvas.drawCircle(300*scaleFactorX,300*scaleFactorY, radius.toFloat(),boxPaint)
                val pointx = result.imageWidth * scaleFactorX - radius * cos(angle)
                val pointy = result.imageHeight * scaleFactorY - radius * sin(angle)

                canvas.drawPoint(pointx.toFloat(), pointy.toFloat(),PointPaint)

            }
        }
        //canvas.drawRect(0f,0f,100*scaleFactorX,200*scaleFactorY,boxPaint)


    }
}