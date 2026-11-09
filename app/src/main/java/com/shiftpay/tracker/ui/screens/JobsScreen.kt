package com.shiftpay.tracker.ui.screens

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shiftpay.tracker.data.Job
import com.shiftpay.tracker.ui.components.EmptyState
import com.shiftpay.tracker.ui.components.JobChip
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel

private val presetColors = listOf(
    0xFF2E7D32.toInt(), 0xFF1565C0.toInt(), 0xFF6A1B9A.toInt(),
    0xFFE65100.toInt(), 0xFFC62828.toInt(), 0xFF00695C.toInt()
)

@Composable
fun JobsScreen(vm: TrackerViewModel) {
    val state by vm.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("20") }
    var otThreshold by remember { mutableStateOf("40") }
    var otMultiplier by remember { mutableStateOf("1.5") }
    var period by remember { mutableStateOf("weekly") }
    var colorIdx by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("Jobs")
        if (state.jobs.isEmpty()) EmptyState("Add your first job below.")
        for (job in state.jobs) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    JobChip(job)
                    Text("${job.name} — \$${job.hourlyRate}/hr · OT ${job.overtimeThresholdHours}h × ${job.overtimeMultiplier} · ${job.payPeriodType}")
                    Row {
                        OutlinedButton(onClick = { vm.deleteJob(job) }) { Text("Delete") }
                    }
                }
            }
        }
        SectionTitle("Add a job")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Job name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = rate, onValueChange = { rate = it }, label = { Text("Hourly rate ($)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = otThreshold, onValueChange = { otThreshold = it }, label = { Text("Overtime threshold (hours/week)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = otMultiplier, onValueChange = { otMultiplier = it }, label = { Text("Overtime multiplier (e.g. 1.5)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("Pay period: weekly / biweekly / semimonthly / monthly") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text("Color:")
        Row {
            presetColors.forEachIndexed { i, c ->
                OutlinedButton(onClick = { colorIdx = i }) {
                    Text(if (i == colorIdx) "●" else "○", color = Color(c))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (name.isBlank()) return@Button
                vm.saveJob(
                    Job(
                        name = name.trim(),
                        color = presetColors[colorIdx],
                        hourlyRate = rate.toDoubleOrNull() ?: 0.0,
                        overtimeThresholdHours = otThreshold.toDoubleOrNull() ?: 40.0,
                        overtimeMultiplier = otMultiplier.toDoubleOrNull() ?: 1.5,
                        payPeriodType = period.trim().lowercase().ifBlank { "weekly" },
                        anchorMillis = System.currentTimeMillis()
                    )
                )
                name = ""; rate = "20"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add job") }
    }
}
