package com.shiftpay.tracker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase, context: Context) {
    private val settingsStore = SettingsStore(context.applicationContext)

    val jobs: Flow<List<Job>> = db.jobDao().observeJobs()
    val shifts: Flow<List<Shift>> = db.shiftDao().observeShifts()
    val templates: Flow<List<ShiftTemplate>> = db.templateDao().observeTemplates()
    val deductions: Flow<List<Deduction>> = db.deductionDao().observeDeductions()
    /** Settings come from DataStore, not Room. */
    val settings: Flow<AppSettings> = settingsStore.settings

    suspend fun getAllJobs(): List<Job> = db.jobDao().getAllJobs()
    suspend fun getJob(id: Long): Job? = db.jobDao().getJob(id)
    suspend fun saveJob(job: Job): Long = db.jobDao().upsert(job)
    suspend fun deleteJob(job: Job) = db.jobDao().delete(job)

    suspend fun getRunningShifts(): List<Shift> = db.shiftDao().getRunningShifts()
    suspend fun getRunningShiftForJob(jobId: Long): Shift? = db.shiftDao().getRunningShiftForJob(jobId)
    suspend fun getShift(id: Long): Shift? = db.shiftDao().getShift(id)
    suspend fun saveShift(shift: Shift): Long = db.shiftDao().upsert(shift)
    suspend fun deleteShift(shift: Shift) = db.shiftDao().delete(shift)
    suspend fun getShiftsInRange(from: Long, to: Long): List<Shift> = db.shiftDao().getShiftsInRange(from, to)

    suspend fun saveTemplate(t: ShiftTemplate): Long = db.templateDao().upsert(t)
    suspend fun deleteTemplate(t: ShiftTemplate) = db.templateDao().delete(t)

    suspend fun getDeductionsForJob(jobId: Long): List<Deduction> = db.deductionDao().getForJob(jobId)
    suspend fun saveDeduction(d: Deduction): Long = db.deductionDao().upsert(d)
    suspend fun deleteDeduction(d: Deduction) = db.deductionDao().delete(d)

    suspend fun getSettings(): AppSettings = settingsStore.getSettings()
    suspend fun saveSettings(s: AppSettings) = settingsStore.saveSettings(s)

    /** Clock in: one running shift per job; a second job is unaffected. */
    suspend fun clockIn(jobId: Long, now: Long = System.currentTimeMillis()): Long {
        if (getRunningShiftForJob(jobId) != null) return -1
        val job = getJob(jobId) ?: return -1
        return saveShift(
            Shift(jobId = jobId, startMillis = now, breakPaid = job.paidBreakByDefault, source = "timer")
        )
    }

    /** Clock out one timer; other running timers keep going. Returns the finished shift. */
    suspend fun clockOut(jobId: Long, now: Long = System.currentTimeMillis()): Shift? {
        val running = getRunningShiftForJob(jobId) ?: return null
        val done = running.copy(endMillis = now)
        saveShift(done)
        return done
    }
}
