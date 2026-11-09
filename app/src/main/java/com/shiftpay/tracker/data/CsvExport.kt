package com.shiftpay.tracker.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** CSV export — always free, no ads (spec §6.3). */
object CsvExport {
    fun build(jobs: List<Job>, shifts: List<Shift>): String {
        val jobById = jobs.associateBy { it.id }
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("date,job,start,end,break_minutes,break_paid,paid_hours,hourly_rate,overtime_hours,tips,miles,mileage_reimbursement,note\n")
        for (s in shifts.sortedBy { it.startMillis }) {
            val job = jobById[s.jobId]
            val paidHours = s.paidMinutes() / 60.0
            val otHours = 0.0 // overtime is weekly; see Pay Summary for the weekly split
            sb.append(dateFmt.format(Date(s.startMillis))).append(',')
            sb.append(csv(job?.name ?: "")).append(',')
            sb.append(timeFmt.format(Date(s.startMillis))).append(',')
            sb.append(if (s.endMillis != null) timeFmt.format(Date(s.endMillis)) else "").append(',')
            sb.append(s.breakMinutes).append(',')
            sb.append(if (s.breakPaid) "yes" else "no").append(',')
            sb.append(String.format(Locale.US, "%.2f", paidHours)).append(',')
            sb.append(job?.hourlyRate ?: 0.0).append(',')
            sb.append(String.format(Locale.US, "%.2f", otHours)).append(',')
            sb.append(s.tips).append(',')
            sb.append(s.miles).append(',')
            sb.append(String.format(Locale.US, "%.2f", s.miles * 0.70)).append(',')
            sb.append(csv(s.note)).append('\n')
        }
        return sb.toString()
    }

    private fun csv(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n'))
            "\"" + value.replace("\"", "\"\"") + "\"" else value
}
