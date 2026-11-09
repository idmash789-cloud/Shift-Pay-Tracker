package com.shiftpay.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shiftpay.tracker.data.AppSettings
import com.shiftpay.tracker.data.Deduction
import com.shiftpay.tracker.data.Job
import com.shiftpay.tracker.data.PayCalculator
import com.shiftpay.tracker.data.PayPeriod
import com.shiftpay.tracker.data.Repository
import com.shiftpay.tracker.data.Shift
import com.shiftpay.tracker.data.ShiftTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TrackerUiState(
    val jobs: List<Job> = emptyList(),
    val shifts: List<Shift> = emptyList(),
    val templates: List<ShiftTemplate> = emptyList(),
    val deductions: List<Deduction> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val nowMillis: Long = System.currentTimeMillis()
)

class TrackerViewModel(private val repo: Repository) : ViewModel() {

    val uiState: StateFlow<TrackerUiState> = combine(
        repo.jobs, repo.shifts, repo.templates, repo.deductions, repo.settings
    ) { jobs, shifts, templates, deductions, settings ->
        TrackerUiState(jobs, shifts, templates, deductions, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackerUiState())

    /**
     * Interstitial frequency cap: at most once per app session AND at least
     * 3 minutes after the previous one, only after a completed action.
     * Never on app open, never while typing or clocking in/out.
     */
    var interstitialShownThisSession: Boolean = false
        private set
    private var lastInterstitialMillis: Long = 0L
    fun canShowInterstitial(now: Long = System.currentTimeMillis()): Boolean =
        !interstitialShownThisSession && now - lastInterstitialMillis >= 180_000L
    fun markInterstitialShown(now: Long = System.currentTimeMillis()) {
        interstitialShownThisSession = true
        lastInterstitialMillis = now
    }

    val runningByJob: Map<Long, Shift>
        get() = uiState.value.shifts.filter { it.isRunning }.associateBy { it.jobId }

    fun clockIn(jobId: Long) = viewModelScope.launch { repo.clockIn(jobId) }
    fun clockOut(jobId: Long) = viewModelScope.launch { repo.clockOut(jobId) }

    fun saveJob(job: Job) = viewModelScope.launch { repo.saveJob(job) }
    fun deleteJob(job: Job) = viewModelScope.launch { repo.deleteJob(job) }
    fun saveShift(shift: Shift) = viewModelScope.launch { repo.saveShift(shift) }
    fun deleteShift(shift: Shift) = viewModelScope.launch { repo.deleteShift(shift) }
    fun saveTemplate(t: ShiftTemplate) = viewModelScope.launch { repo.saveTemplate(t) }
    fun deleteTemplate(t: ShiftTemplate) = viewModelScope.launch { repo.deleteTemplate(t) }
    fun saveDeduction(d: Deduction) = viewModelScope.launch { repo.saveDeduction(d) }
    fun deleteDeduction(d: Deduction) = viewModelScope.launch { repo.deleteDeduction(d) }
    fun saveSettings(s: AppSettings) = viewModelScope.launch { repo.saveSettings(s) }

    fun paySummaryFor(job: Job, now: Long = System.currentTimeMillis()): PayCalculator.PaySummary {
        val range = PayPeriod.rangeFor(job.payPeriodType, job.anchorMillis, now)
        val shifts = uiState.value.shifts.filter {
            it.jobId == job.id && it.startMillis in range.startMillis until range.endMillis
        }
        // Weekly OT is computed per work week; for a pay period we sum the
        // weekly summaries that overlap it by evaluating each week separately.
        // Simplification for MVP: evaluate the period's shifts as one weekly
        // bucket per week-start — PayCalculator handles the reset internally
        // when shifts span multiple weeks because it re-keys on week start.
        return PayCalculator.weeklyPay(
            job, shifts, uiState.value.deductions, uiState.value.settings.mileageRate, now
        )
    }

    fun nextPayday(job: Job, now: Long = System.currentTimeMillis()): Long =
        PayPeriod.nextPayday(job.payPeriodType, job.anchorMillis, now)

    /** Apply a template to a date: creates a finished shift (incl. overnight). */
    fun applyTemplate(template: ShiftTemplate, dayMillis: Long) = viewModelScope.launch {
        val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
        cal.set(Calendar.HOUR_OF_DAY, template.startHour)
        cal.set(Calendar.MINUTE, template.startMinute)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, template.endHour)
        cal.set(Calendar.MINUTE, template.endMinute)
        var end = cal.timeInMillis
        if (end <= start) end += 86_400_000L // overnight shift stays one shift
        repo.saveShift(Shift(jobId = template.jobId, startMillis = start, endMillis = end, source = "template"))
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TrackerViewModel(repo) as T
    }
}
