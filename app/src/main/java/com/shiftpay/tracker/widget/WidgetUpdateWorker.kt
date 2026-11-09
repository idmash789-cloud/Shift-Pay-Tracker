package com.shiftpay.tracker.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background work (WorkManager): refresh the home-screen widget once a day
 * so its timer status and payday countdown stay fresh without polling,
 * location or a foreground service. No notification, no network.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val manager = AppWidgetManager.getInstance(applicationContext)
            val ids = manager.getAppWidgetIds(ComponentName(applicationContext, ClockWidget::class.java))
            val intent = Intent(applicationContext, ClockWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            applicationContext.sendBroadcast(intent)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
