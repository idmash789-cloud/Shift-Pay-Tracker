package com.shiftpay.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.shiftpay.tracker.data.Deduction
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel

@Composable
fun SettingsScreen(vm: TrackerViewModel) {
    val state by vm.uiState.collectAsState()
    var mileage by remember(state.settings.mileageRate) { mutableStateOf(state.settings.mileageRate.toString()) }
    var dedName by remember { mutableStateOf("") }
    var dedAmount by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("Settings")
        Text("Fully offline. No account, no sync, no online/offline indicator — by design.")
        OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("Mileage rate ($ per mile)") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                vm.saveSettings(state.settings.copy(mileageRate = mileage.toDoubleOrNull() ?: 0.70))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save mileage rate") }

        SectionTitle("Deductions (per pay period)")
        for (d in state.deductions) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Text("${d.name} — \$${d.amount}", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.deleteDeduction(d) }) { Text("Delete") }
                }
            }
        }
        if (state.jobs.isNotEmpty()) {
            OutlinedTextField(value = dedName, onValueChange = { dedName = it }, label = { Text("Deduction name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dedAmount, onValueChange = { dedAmount = it }, label = { Text("Amount ($)") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (dedName.isBlank()) return@Button
                    vm.saveDeduction(Deduction(jobId = state.jobs.first().id, name = dedName.trim(), amount = dedAmount.toDoubleOrNull() ?: 0.0))
                    dedName = ""; dedAmount = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add deduction") }
        }
        SectionTitle("About this MVP")
        Text("Version 1.0.0 — Shift & Pay Tracker. Overtime 40h × 1.5x by default, adjustable per job. All pay figures are estimates.")
    }
}
