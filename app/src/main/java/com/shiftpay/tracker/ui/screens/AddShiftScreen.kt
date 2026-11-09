package com.shiftpay.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftpay.tracker.data.Shift
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private fun parseDateTime(date: String, time: String): Long? {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        fmt.isLenient = false
        fmt.parse("$date $time")?.time
    } catch (e: Exception) { null }
}

@Composable
fun AddShiftScreen(vm: TrackerViewModel, shiftId: Long?, onDone: () -> Unit) {
    val state by vm.uiState.collectAsState()
    val existing = state.shifts.firstOrNull { it.id == shiftId }
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    var jobId by remember { mutableStateOf(existing?.jobId ?: state.jobs.firstOrNull()?.id ?: 0L) }
    var date by remember { mutableStateOf(existing?.let { dateFmt.format(it.startMillis) } ?: dateFmt.format(System.currentTimeMillis())) }
    var start by remember { mutableStateOf(existing?.let { timeFmt.format(it.startMillis) } ?: "09:00") }
    var end by remember { mutableStateOf(existing?.endMillis?.let { timeFmt.format(it) } ?: "17:00") }
    var breakMinutes by remember { mutableStateOf(existing?.breakMinutes?.toString() ?: "30") }
    var breakPaid by remember { mutableStateOf(existing?.breakPaid ?: true) }
    var tips by remember { mutableStateOf(existing?.tips?.toString() ?: "0") }
    var miles by remember { mutableStateOf(existing?.miles?.toString() ?: "0") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(state.jobs) {
        if (jobId == 0L) jobId = state.jobs.firstOrNull()?.id ?: 0L
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle(if (existing == null) "Add shift" else "Edit shift")
        if (state.jobs.isEmpty()) {
            Text("Add a job first on the Jobs tab.")
            return@Column
        }
        Text("Job: ${state.jobs.firstOrNull { it.id == jobId }?.name ?: ""} (first job is used; change the job on the Jobs tab order if needed)")
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End (HH:mm) — next day if earlier than start") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = breakMinutes, onValueChange = { breakMinutes = it }, label = { Text("Break (minutes)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = tips, onValueChange = { tips = it }, label = { Text("Tips ($)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = miles, onValueChange = { miles = it }, label = { Text("Miles") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
        if (error.isNotEmpty()) Text(error)
        Button(
            onClick = {
                var startMillis = parseDateTime(date, start)
                var endMillis = parseDateTime(date, end)
                if (startMillis == null || endMillis == null) { error = "Check the date/time format."; return@Button }
                if (endMillis <= startMillis) endMillis += 86_400_000L // overnight: one whole shift
                val job = state.jobs.firstOrNull { it.id == jobId } ?: return@Button
                vm.saveShift(
                    Shift(
                        id = existing?.id ?: 0,
                        jobId = job.id,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        breakMinutes = breakMinutes.toIntOrNull() ?: 0,
                        breakPaid = breakPaid,
                        tips = tips.toDoubleOrNull() ?: 0.0,
                        miles = miles.toDoubleOrNull() ?: 0.0,
                        note = note,
                        source = existing?.source ?: "manual"
                    )
                )
                onDone()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save shift") }
    }
}
