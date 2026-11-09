package com.shiftpay.tracker.data

import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Pay periods (spec §6): weekly, biweekly, semimonthly (1–15 / 16–end),
 * monthly. All computed from the device date — fully offline.
 */
object PayPeriod {

    data class Range(val startMillis: Long, val endMillis: Long)

    fun rangeFor(type: String, anchorMillis: Long, nowMillis: Long): Range {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (type) {
            "biweekly" -> {
                val day = TimeUnit.MILLISECONDS.toDays(nowMillis - anchorMillis)
                val periods = if (day >= 0) day / 14 else (day - 13) / 14
                val start = anchorMillis + periods * 14L * 86_400_000L
                Range(start, start + 14L * 86_400_000L)
            }
            "semimonthly" -> {
                if (cal.get(Calendar.DAY_OF_MONTH) <= 15) {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val start = startOfDay(cal.timeInMillis)
                    cal.set(Calendar.DAY_OF_MONTH, 16)
                    Range(start, startOfDay(cal.timeInMillis))
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, 16)
                    val start = startOfDay(cal.timeInMillis)
                    cal.add(Calendar.MONTH, 1); cal.set(Calendar.DAY_OF_MONTH, 1)
                    Range(start, startOfDay(cal.timeInMillis))
                }
            }
            "monthly" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = startOfDay(cal.timeInMillis)
                cal.add(Calendar.MONTH, 1)
                Range(start, startOfDay(cal.timeInMillis))
            }
            else -> { // weekly
                val day = TimeUnit.MILLISECONDS.toDays(nowMillis - anchorMillis)
                val periods = if (day >= 0) day / 7 else (day - 6) / 7
                val start = anchorMillis + periods * 7L * 86_400_000L
                Range(start, start + 7L * 86_400_000L)
            }
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Next payday after [nowMillis] for this period type + anchor. */
    fun nextPayday(type: String, anchorMillis: Long, nowMillis: Long): Long {
        var range = rangeFor(type, anchorMillis, nowMillis)
        var guard = 0
        while (range.endMillis <= nowMillis && guard++ < 60) {
            range = rangeFor(type, anchorMillis, range.endMillis + 1)
        }
        return range.endMillis
    }

    fun daysUntil(millis: Long, nowMillis: Long): Long =
        TimeUnit.MILLISECONDS.toDays(millis - nowMillis).coerceAtLeast(0)

    /** "rotation:3on2off" → (workDays=3, offDays=2). Null if not a rotation pattern. */
    fun parseRotation(pattern: String): Pair<Int, Int>? {
        val m = Regex("""rotation:(\d+)on(\d+)off""").find(pattern) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }
}
