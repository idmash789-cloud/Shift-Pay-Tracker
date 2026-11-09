package com.shiftpay.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiftpay.tracker.ui.components.EmptyState
import com.shiftpay.tracker.ui.components.NativeAdCard
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.components.formatDate
import com.shiftpay.tracker.ui.components.formatMoney
import com.shiftpay.tracker.ui.components.formatTime
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel

@Composable
fun HistoryScreen(vm: TrackerViewModel, onEdit: (Long) -> Unit) {
    val state by vm.uiState.collectAsState()
    val jobById = state.jobs.associateBy { it.id }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionTitle("Shift history")
        if (state.shifts.isEmpty()) { EmptyState("No shifts yet."); return@Column }
        LazyColumn {
            itemsIndexed(state.shifts) { index, shift ->
                // One clearly-labelled native ad after the 3rd item — it never
                // pretends to be a shift.
                if (index == 3) NativeAdCard()
                val job = jobById[shift.jobId]
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${job?.name ?: "Job"} — ${formatDate(shift.startMillis)}")
                        Text(
                            if (shift.isRunning) "Running since ${formatTime(shift.startMillis)}"
                            else "${formatTime(shift.startMillis)} – ${shift.endMillis?.let { formatTime(it) } ?: ""} · ${shift.paidMinutes()} min paid"
                        )
                        Text("Tips ${formatMoney(shift.tips)} · ${shift.miles} mi · ${shift.note}")
                        Row {
                            if (!shift.isRunning) {
                                OutlinedButton(onClick = { onEdit(shift.id) }) { Text("Edit") }
                            }
                            OutlinedButton(onClick = { vm.deleteShift(shift) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
