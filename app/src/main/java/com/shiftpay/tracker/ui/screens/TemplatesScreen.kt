package com.shiftpay.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.shiftpay.tracker.data.ShiftTemplate
import com.shiftpay.tracker.ui.components.EmptyState
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel

@Composable
fun TemplatesScreen(vm: TrackerViewModel) {
    val state by vm.uiState.collectAsState()
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("17:00") }
    var pattern by remember { mutableStateOf("weekly:MON,WED,FRI") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("Shift templates")
        Text("One tap on a template creates a shift for the chosen day — including overnight shifts.")
        if (state.templates.isEmpty()) EmptyState("No templates yet.")
        for (t in state.templates) {
            val job = state.jobs.firstOrNull { it.id == t.jobId }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${job?.name ?: "Job"} — ${t.startHour}:${"%02d".format(t.startMinute)} to ${t.endHour}:${"%02d".format(t.endMinute)}")
                    Text(t.pattern)
                    OutlinedButton(onClick = { vm.applyTemplate(t, System.currentTimeMillis()) }) { Text("Apply today") }
                    OutlinedButton(onClick = { vm.deleteTemplate(t) }) { Text("Delete") }
                }
            }
        }
        SectionTitle("Add a template")
        if (state.jobs.isEmpty()) { Text("Add a job first."); return@Column }
        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End (HH:mm)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Pattern — weekly:MON,WED,FRI or rotation:3on2off") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val startParts = start.split(":").mapNotNull { it.toIntOrNull() }
                val endParts = end.split(":").mapNotNull { it.toIntOrNull() }
                if (startParts.size != 2 || endParts.size != 2) return@Button
                val sh = startParts[0]; val sm = startParts[1]
                val eh = endParts[0]; val em = endParts[1]
                vm.saveTemplate(
                    ShiftTemplate(jobId = state.jobs.first().id, startHour = sh, startMinute = sm, endHour = eh, endMinute = em, pattern = pattern)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Add template") }
    }
}
