package com.shiftpay.tracker.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One-page pay-stub PDF (spec §6.4). Plain summary only — no company logo,
 * no account numbers, nothing that looks like an official employer pay stub.
 * Every PDF carries the estimate label + disclaimer.
 */
object PdfExport {
    fun build(
        context: Context,
        job: Job,
        summary: PayCalculator.PaySummary,
        periodStart: Long,
        periodEnd: Long,
        paydayMillis: Long
    ): File {
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val title = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 13f }
        val small = Paint().apply { textSize = 10f }
        var y = 60f
        fun line(text: String, paint: Paint = body) { canvas.drawText(text, 48f, y, paint); y += 22f }
        line("Pay Summary — ESTIMATE", title)
        line("Job: ${job.name}")
        line("Period: ${dateFmt.format(Date(periodStart))} – ${dateFmt.format(Date(periodEnd - 1))}")
        line("Payday: ${dateFmt.format(Date(paydayMillis))}")
        y += 8f
        line("Regular: ${summary.regularHours} hrs — \$${summary.regularPay}")
        line("Overtime: ${summary.overtimeHours} hrs — \$${summary.overtimePay}")
        if (summary.differentialPay > 0) line("Night / holiday differential: \$${summary.differentialPay}")
        line("Tips: \$${summary.tips}")
        line("Gross (estimate): \$${summary.gross}")
        line("Deductions: -\$${summary.deductions}")
        line("Net (estimate): \$${summary.net}")
        line("Mileage (separate): ${summary.totalMiles} mi — \$${summary.mileagePay}")
        y += 12f
        line("Estimate only — not legal or tax advice.", small)
        doc.finishPage(page)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "pay-stub-${job.name.replace(" ", "-")}-${periodStart}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
