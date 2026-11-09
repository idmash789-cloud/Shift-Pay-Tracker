package com.shiftpay.tracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.AdRequest
import com.shiftpay.tracker.data.CsvExport
import com.shiftpay.tracker.data.PayPeriod
import com.shiftpay.tracker.data.PdfExport
import com.shiftpay.tracker.ui.components.AdBanner
import com.shiftpay.tracker.ui.components.SectionTitle
import com.shiftpay.tracker.ui.components.formatMoney
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel
import com.shiftpay.tracker.util.AdMobHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaySummaryScreen(vm: TrackerViewModel) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }

    val job = state.jobs.firstOrNull { it.id == selectedJobId } ?: state.jobs.firstOrNull()
    LaunchedEffect(state.jobs) { if (selectedJobId == null) selectedJobId = state.jobs.firstOrNull()?.id }

    // Interstitial: at most once per session AND at least 3 minutes after
    // the previous one, only after a completed view — never on app open.
    LaunchedEffect(job?.id) {
        if (job != null && vm.canShowInterstitial()) {
            AdMobHelper.loadInterstitial(context) { ad ->
                ad?.show(context as android.app.Activity)
                vm.markInterstitialShown()
            }
        }
    }

    fun shareFile(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionTitle("Pay summary — ESTIMATE")
        Text("Estimate only — not legal or tax advice.", style = MaterialTheme.typography.bodySmall)
        if (job == null) { Text("Add a job first."); return@Column }
        val now = System.currentTimeMillis()
        val range = PayPeriod.rangeFor(job.payPeriodType, job.anchorMillis, now)
        val summary = vm.paySummaryFor(job, now)
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(job.name, style = MaterialTheme.typography.titleMedium)
                Text("Period: ${fmt.format(Date(range.startMillis))} – ${fmt.format(Date(range.endMillis - 1))}")
                Text("Regular: ${summary.regularHours} hrs — ${formatMoney(summary.regularPay)}")
                Text("Overtime: ${summary.overtimeHours} hrs — ${formatMoney(summary.overtimePay)}")
                if (summary.differentialPay > 0) Text("Night / holiday: ${formatMoney(summary.differentialPay)}")
                Text("Tips: ${formatMoney(summary.tips)}")
                Text("Gross (estimate): ${formatMoney(summary.gross)}", style = MaterialTheme.typography.titleMedium)
                Text("Deductions: -${formatMoney(summary.deductions)}")
                Text("Net (estimate): ${formatMoney(summary.net)}", style = MaterialTheme.typography.titleMedium)
                Text("Mileage (separate): ${summary.totalMiles} mi — ${formatMoney(summary.mileagePay)}")
            }
        }
        Button(
            onClick = {
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "shifts-${job.name.replace(" ", "-")}-${range.startMillis}.csv")
                file.writeText(CsvExport.build(state.jobs, state.shifts.filter { it.jobId == job.id }))
                shareFile(file, "text/csv")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export CSV — free") }
        OutlinedButton(
            onClick = {
                // Rewarded ad gates the PDF (spec §6.4). CSV stays free if the ad fails.
                RewardedAd.load(
                    context, AdMobHelper.REWARDED_TEST_ID, AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                                    message = "Ad could not be shown — CSV export is still free."
                                }
                            }
                            ad.show(context as android.app.Activity) { _ ->
                                val file = PdfExport.build(context, job, summary, range.startMillis, range.endMillis, vm.nextPayday(job, now))
                                shareFile(file, "application/pdf")
                            }
                        }
                        override fun onAdFailedToLoad(e: LoadAdError) {
                            message = "Rewarded ad not available right now — CSV export is still free."
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export PDF pay stub — watch a rewarded ad") }
        if (message.isNotEmpty()) Text(message)
        AdBanner()
    }
}
