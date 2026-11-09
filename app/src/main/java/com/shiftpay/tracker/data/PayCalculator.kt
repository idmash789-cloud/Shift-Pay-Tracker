package com.shiftpay.tracker.data

import java.util.Calendar
import kotlin.math.roundToLong

/**
 * Pure pay math — no Android, no Room. Testable on its own.
 *
 * Spec §5.3 verified example: one job, $20/hr, 1.5x overtime,
 * 45 paid hours in a week, $60 tips, 100 miles @ $0.70, $25 deduction
 * → regular $800.00 + overtime $150.00 + tips $60.00 = gross $1,010.00,
 * net $985.00, mileage $70.00 shown separately.
 */
object PayCalculator {

    data class PaySummary(
        val regularHours: Double,
        val overtimeHours: Double,
        val regularPay: Double,
        val overtimePay: Double,
        val differentialPay: Double,
        val tips: Double,
        val gross: Double,
        val deductions: Double,
        val net: Double,
        val mileagePay: Double,
        val totalMiles: Double
    )

    fun roundMoney(value: Double): Double = (value * 100).roundToLong() / 100.0

    fun isNightShift(shift: Shift): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = shift.startMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }

    fun isHolidayShift(shift: Shift): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = shift.startMillis }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }

    /** Start of the work week containing [millis], per [weekStartDay]. */
    fun weekStart(millis: Long, weekStartDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != weekStartDay) cal.add(Calendar.DAY_OF_MONTH, -1)
        return cal.timeInMillis
    }

    /**
     * Weekly pay for one job. Overtime is per job per week, in chronological
     * order — hours beyond [Job.overtimeThresholdHours] in that week are OT.
     */
    fun weeklyPay(
        job: Job,
        shifts: List<Shift>,
        deductions: List<Deduction>,
        mileageRate: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): PaySummary {
        val finished = shifts.filter { it.jobId == job.id && it.endMillis != null }
            .sortedBy { it.startMillis }
        var regularHours = 0.0
        var overtimeHours = 0.0
        var regularPay = 0.0
        var overtimePay = 0.0
        var differentialPay = 0.0
        var tips = 0.0
        var miles = 0.0
        var paidSoFarThisWeek = 0.0
        var currentWeek = -1L

        for (shift in finished) {
            val week = weekStart(shift.startMillis, job.weekStartDay)
            if (week != currentWeek) { currentWeek = week; paidSoFarThisWeek = 0.0 }
            val hours = shift.paidMinutes(nowMillis) / 60.0
            val regularThisShift = (job.overtimeThresholdHours - paidSoFarThisWeek)
                .coerceIn(0.0, hours)
            val overtimeThisShift = (hours - regularThisShift).coerceAtLeast(0.0)
            paidSoFarThisWeek += hours
            regularHours += regularThisShift
            overtimeHours += overtimeThisShift
            regularPay += regularThisShift * job.hourlyRate
            overtimePay += overtimeThisShift * job.hourlyRate * job.overtimeMultiplier
            var diff = 0.0
            if (isNightShift(shift)) diff += job.nightDifferential * hours
            if (isHolidayShift(shift)) diff += job.holidayDifferential * hours
            differentialPay += diff
            tips += shift.tips
            miles += shift.miles
        }
        val gross = roundMoney(regularPay + overtimePay + differentialPay + tips)
        val deductionTotal = roundMoney(deductions.filter { it.jobId == job.id }.sumOf { it.amount })
        return PaySummary(
            regularHours = roundMoney(regularHours),
            overtimeHours = roundMoney(overtimeHours),
            regularPay = roundMoney(regularPay),
            overtimePay = roundMoney(overtimePay),
            differentialPay = roundMoney(differentialPay),
            tips = roundMoney(tips),
            gross = gross,
            deductions = deductionTotal,
            net = roundMoney(gross - deductionTotal),
            mileagePay = roundMoney(miles * mileageRate),
            totalMiles = roundMoney(miles)
        )
    }
}
