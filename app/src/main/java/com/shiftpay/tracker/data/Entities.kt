package com.shiftpay.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val hourlyRate: Double,
    val overtimeMultiplier: Double = 1.5,
    val overtimeThresholdHours: Double = 40.0,
    /** Calendar.MONDAY etc. — first day of the work week for this job. */
    val weekStartDay: Int = Calendar.MONDAY,
    /** weekly | biweekly | semimonthly | monthly */
    val payPeriodType: String = "weekly",
    /** Anchor: first payday / first period start, epoch millis. */
    val anchorMillis: Long = System.currentTimeMillis(),
    val nightDifferential: Double = 0.0,
    val holidayDifferential: Double = 0.0,
    val paidBreakByDefault: Boolean = true,
    val archived: Boolean = false
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val startMillis: Long,
    /** Null while the timer is running. */
    val endMillis: Long? = null,
    val breakMinutes: Int = 0,
    val breakPaid: Boolean = true,
    val tips: Double = 0.0,
    val miles: Double = 0.0,
    val note: String = "",
    /** timer | manual | template */
    val source: String = "manual"
) {
    val isRunning: Boolean get() = endMillis == null
    /** Paid minutes: elapsed minus unpaid break. */
    fun paidMinutes(nowMillis: Long = System.currentTimeMillis()): Long {
        val end = endMillis ?: nowMillis
        val elapsed = (end - startMillis) / 60_000L
        val unpaid = if (breakPaid) 0 else breakMinutes
        return (elapsed - unpaid).coerceAtLeast(0)
    }
}

@Entity(tableName = "shift_templates")
data class ShiftTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    /** "weekly:MON,WED,FRI" or "rotation:3on2off" — see PayPeriod.parseRotation */
    val pattern: String = "",
    val active: Boolean = true
)

@Entity(tableName = "deductions")
data class Deduction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val name: String,
    val amount: Double
)

/* AppSettings lives in DataStore — see SettingsStore.kt. It is deliberately
 * NOT a Room entity: settings are preferences, shift/job/pay data is Room. */
