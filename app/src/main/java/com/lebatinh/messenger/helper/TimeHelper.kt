package com.lebatinh.messenger.helper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimeHelper {
    fun formatElapsedTime(timeSend: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timeSend

        // Chuyển đổi sang các đơn vị thời gian
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "$seconds giây trước"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 7 -> "$days ngày trước"
            else -> {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateFormat.format(Date(timeSend))
            }
        }
    }

    fun formatTimeSend(timeSend: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timeSend

        val days = TimeUnit.MILLISECONDS.toDays(diff)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        return when (days) {
            0L -> timeFormat.format(Date(timeSend))
            in 1..6 -> "${dayOfWeekFormat.format(Date(timeSend))} LÚC ${
                timeFormat.format(
                    Date(
                        timeSend
                    )
                )
            }"

            else -> dateFormat.format(Date(timeSend))
        }
    }
}