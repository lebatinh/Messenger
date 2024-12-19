package com.lebatinh.messenger.animation

import android.animation.ValueAnimator
import com.airbnb.lottie.LottieAnimationView

class CustomLottieAnimation(
    private val lottieView: LottieAnimationView
) {
    private var currentProgress: Float = 0f

    /**
     * Di chuyển mắt theo số ký tự, dùng ValueAnimator để làm mượt
     */
    fun moveEyes(charCount: Int, maxChar: Int = 10) {
        val targetProgress = when {
            charCount == 0 -> 0.05f
            charCount <= maxChar -> 0.05f + (charCount - 1) * (0.2f - 0.05f) / (maxChar - 1)
            else -> 0.2f
        }

        ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = 500 // Thời gian chuyển động
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Float
                lottieView.progress = currentProgress
            }
            start()
        }
    }

    /**
     * Chuyển đổi trạng thái che/bỏ che mắt, dùng ValueAnimator
     */
    fun toggleEyeCover(isPasswordHidden: Boolean) {
        val startProgress = if (isPasswordHidden) 0.37f else 0.52f
        val endProgress = if (isPasswordHidden) 0.5f else 0.66f

        ValueAnimator.ofFloat(startProgress, endProgress).apply {
            duration = 500
            addUpdateListener { animator ->
                lottieView.progress = animator.animatedValue as Float
            }
            start()
        }
    }

    /**
     * Đưa mắt về trạng thái mặc định
     */
    fun resetEyesToDefault() {
        ValueAnimator.ofFloat(currentProgress, 0.2f).apply {
            duration = 500
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Float
                lottieView.progress = currentProgress
            }
            start()
        }
    }

    /**
     * Đưa mắt về trạng thái đầu tiên
     */
    fun resetEyesToInitial() {
        ValueAnimator.ofFloat(currentProgress, 0.05f).apply {
            duration = 500
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Float
                lottieView.progress = currentProgress
            }
            start()
        }
    }
}