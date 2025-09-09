package com.example.background_remove.adsManager

object TimeUtils {
    var Time_Interval: Long = 30000
    private var lastAdShownTime: Long = 0

    fun isIntervalElapsed(): Boolean {
        return true
    }

    fun updateLastAdShownTime() {
        lastAdShownTime = System.currentTimeMillis()

    }

    fun getLastAdShownTime(): Long {
        return lastAdShownTime
    }
}