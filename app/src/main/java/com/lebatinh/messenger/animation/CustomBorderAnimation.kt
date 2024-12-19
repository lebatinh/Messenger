package com.lebatinh.messenger.animation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.DashPathEffect
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RoundRectShape
import android.view.View
import android.view.animation.LinearInterpolator

class CustomBorderAnimation {

    /**
     * animation cho viền view hình hộp
     */
    fun runBorderAnimation(
        view: View,
        context: Context,
        borderWidth: Float,
        dashWidth: Float,
        dashGap: Float,
        cornerRadius: Float,
        duration: Long
    ) {
        val radii = floatArrayOf(
            cornerRadius, cornerRadius, // Top-left
            cornerRadius, cornerRadius, // Top-right
            cornerRadius, cornerRadius, // Bottom-right
            cornerRadius, cornerRadius  // Bottom-left
        )
        val roundRectShape = RoundRectShape(radii, null, null)

        // Tạo ShapeDrawable với viền nét đứt
        val shapeDrawable = ShapeDrawable(roundRectShape).apply {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = context.getColor(android.R.color.holo_blue_dark)
            paint.pathEffect = DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        }

        // Đặt background cho View
        view.background = shapeDrawable

        // Tạo animator để thay đổi phase
        val animator = ValueAnimator.ofFloat(0f, dashWidth + dashGap).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val phase = animation.animatedValue as Float
                shapeDrawable.paint.pathEffect =
                    DashPathEffect(floatArrayOf(dashWidth, dashGap), phase)
                view.invalidate()
            }
        }
        animator.start()
    }

    /**
     * animation cho view hình tròn
     */
    fun runCircularBorderAnimation(
        view: View,
        context: Context,
        borderWidth: Float,
        dashWidth: Float,
        dashGap: Float,
        duration: Long
    ) {
        // Tạo ShapeDrawable với OvalShape (hình tròn)
        val shapeDrawable = ShapeDrawable(OvalShape()).apply {
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = context.getColor(android.R.color.holo_blue_dark)
            paint.pathEffect = DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
        }

        // Đặt background cho View
        view.background = shapeDrawable

        // Tạo animator để thay đổi phase của DashPathEffect
        val animator = ValueAnimator.ofFloat(0f, dashWidth + dashGap).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val phase = animation.animatedValue as Float
                shapeDrawable.paint.pathEffect =
                    DashPathEffect(floatArrayOf(dashWidth, dashGap), phase)
                view.invalidate()
            }
        }
        animator.start()
    }
}