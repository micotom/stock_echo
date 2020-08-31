package com.funglejunk.stockecho.view

import android.content.Context
import android.graphics.*
import arrow.fx.IO
import timber.log.Timber

class ChartCanvas(
    context: Context
) : Canvas() {

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density
        color = Color.GRAY
        pathEffect = DashPathEffect(floatArrayOf(3f, 12f), 50f)
    }

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = context.resources.displayMetrics.density * 1.5f
        color = Color.DKGRAY
    }

    private val path = Path()

    fun draw(
        data: List<Float>,
        viewWidth: Int,
        viewHeight: Int
    ): IO<Unit> = IO {
        drawLine(0f, 0f, viewWidth.toFloat(), 0f, borderPaint)
        drawLine(0f, viewHeight.toFloat(), viewWidth.toFloat(), viewHeight.toFloat(), borderPaint)

        if (data.isEmpty()) {
            return@IO
        }

        path.reset()
        path.moveTo(0f, 0f)
        val xSpreadFactor = viewWidth / data.size
        val normalisedData = data.yNormalise(viewHeight - 16)
        normalisedData.forEachIndexed { index, y ->
            val x = (index * xSpreadFactor).toFloat()
            if (index == 0) {
                path.moveTo(x, viewHeight - y)
            }
            path.lineTo(x, viewHeight - y)
            path.moveTo(x, viewHeight - y)
        }
        drawPath(path, paint)
    }

    private fun List<Float>.yNormalise(canvasHeight: Int) = run {
        val maxValueY = maxByOrNull { it }!!
        val minValueY = minByOrNull { it }!!
        val verticalSpan = when (val diff = maxValueY - minValueY) {
            0f -> 1f
            else -> diff
        }
        val factorY = canvasHeight / verticalSpan
        map {
            (it - minValueY) * factorY + 8
        }
    }
    
}