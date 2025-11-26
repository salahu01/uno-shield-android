package com.unoshield.mdm.ui

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable

/**
 * Custom drawable to draw grid lines on the background
 */
class GridLinesDrawable(
    private val width: Int,
    private val height: Int,
    private val lineColor: Int
) : Drawable() {
    
    private val paint = Paint().apply {
        color = lineColor
        strokeWidth = 0.5f // Thinner lines
        style = Paint.Style.STROKE
        alpha = 60 // More transparent/lighter lines
    }
    
    // Convert 30dp to pixels for smaller grid (reduced from 60dp)
    private val gridSpacing = 30f * Resources.getSystem().displayMetrics.density
    
    override fun draw(canvas: Canvas) {
        // Draw vertical lines
        var x = gridSpacing
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            x += gridSpacing
        }
        
        // Draw horizontal lines
        var y = gridSpacing
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += gridSpacing
        }
    }
    
    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        paint.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int {
        return android.graphics.PixelFormat.TRANSLUCENT
    }
}

