package com.aedev.aetranzit.domain.usecase

import java.util.Calendar
import java.util.TimeZone

enum class AnalyticsDateRange(
    val chipLabel: String,
    val reportLabel: String
) {
    TODAY(chipLabel = "Today", reportLabel = "Today"),
    LAST_7_DAYS(chipLabel = "7 Days", reportLabel = "Last 7 Days"),
    LAST_30_DAYS(chipLabel = "30 Days", reportLabel = "Last 30 Days"),
    ALL_TIME(chipLabel = "All Time", reportLabel = "All Time");

    fun cutoffEpochMillis(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        return when (this) {
            TODAY -> startOfDayMillis(nowMillis, timeZone)
            LAST_7_DAYS -> shiftDaysFromStartOfDay(nowMillis, daysDelta = -6, timeZone = timeZone)
            LAST_30_DAYS -> shiftDaysFromStartOfDay(nowMillis, daysDelta = -29, timeZone = timeZone)
            ALL_TIME -> null
        }
    }

    fun contains(
        epochMillis: Long,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val cutoff = cutoffEpochMillis(nowMillis, timeZone) ?: return true
        return epochMillis >= cutoff
    }

    companion object {
        fun startOfDayMillis(
            epochMillis: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = epochMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        private fun shiftDaysFromStartOfDay(
            nowMillis: Long,
            daysDelta: Int,
            timeZone: TimeZone
        ): Long {
            val calendar = Calendar.getInstance(timeZone).apply {
                timeInMillis = startOfDayMillis(nowMillis, timeZone)
                add(Calendar.DAY_OF_YEAR, daysDelta)
            }
            return calendar.timeInMillis
        }
    }
}
