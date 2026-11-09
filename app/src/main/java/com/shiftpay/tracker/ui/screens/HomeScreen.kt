package com.shiftpay.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftpay.tracker.data.PayPeriod
import com.shiftpay.tracker.ui.components.AdBanner
import com.shiftpay.tracker.ui.components.EmptyState
import com.shiftpay.tracker.ui.components.JobChip
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.components.formatMoney
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(vm: TrackerViewModel, onAddShift: () -> Unit, onTemplates: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val running = vm.runningByJob
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        SectionTitle("Shift & Pay Tracker")
        Text("All amounts are estimates.", style = MaterialTheme.typography.bodySmall)

        // Payday countdown — nearest payday across jobs.
        val now = System.currentTimeMillis()
        val nearest = state.jobs.minByOrNull { vm.nextPayday(it, now) }
        if (nearest != null) {
            val payday = vm.nextPayday(nearest, now)
            val days = PayPeriod.daysUntil(payday, now)
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Next payday: ${nearest.name}", style = MaterialTheme.typography.titleMedium)
                    Text("$days day(s) — ${fmt.format(Date(payday))}")
                }
            }
        }

        SectionTitle("Your jobs — tap to clock in / out")
        if (state.jobs.isEmpty()) {
            EmptyState("No jobs yet. Add your first job on the Jobs tab.")
        }
        for (job in state.jobs) {
            val shift = running[job.id]
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        JobChip(job)
                        Text(formatMoney(job.hourlyRate) + "/hr")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (shift != null) {
                        val mins = TimeUnit.MILLISECONDS.toMinutes(now - shift.startMillis)
                        Text("Running since ${SimpleDateFormat("HH:mm", Locale.US).format(Date(shift.startMillis))} — ${mins} min")
                        Button(onClick = { vm.clockOut(job.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Clock Out — ${job.name}")
                        }
                    } else {
                        Button(onClick = { vm.clockIn(job.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Clock In — ${job.name}")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAddShift) { Text("Add shift manually") }
            OutlinedButton(onClick = onTemplates) { Text("Templates") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Banner sits BELOW the clock in/out buttons — never covering them.
        AdBanner()
        Text("Estimate only — not legal or tax advice.", style = MaterialTheme.typography.bodySmall)
    }
}
