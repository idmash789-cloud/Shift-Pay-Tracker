package com.shiftpay.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.shiftpay.tracker.MainActivity
import com.shiftpay.tracker.R
import com.shiftpay.tracker.data.AppDatabase
import com.shiftpay.tracker.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home-screen widget (spec §3.4): clock in / out in one tap without
 * opening the app. Acts on the default job, or the first active job.
 */
class ClockWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_CLOCK_IN = "com.shiftpay.tracker.WIDGET_CLOCK_IN"
        const val ACTION_CLOCK_OUT = "com.shiftpay.tracker.WIDGET_CLOCK_OUT"
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, manager, id, "No active timer")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLOCK_IN || intent.action == ACTION_CLOCK_OUT) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = Repository(AppDatabase.get(context), context)
                    val settings = repo.getSettings()
                    val jobs = repo.getAllJobs()
                    val job = jobs.firstOrNull { it.id == settings.defaultJobId } ?: jobs.firstOrNull()
                    if (job != null) {
                        if (intent.action == ACTION_CLOCK_IN) repo.clockIn(job.id)
                        else repo.clockOut(job.id)
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(
                        android.content.ComponentName(context, ClockWidget::class.java)
                    )
                    val running = repo.getRunningShifts()
                    val status = if (running.isEmpty()) "No active timer"
                        else "${running.size} timer(s) running"
                    for (id in ids) updateWidget(context, manager, id, status)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int, status: String) {
        val views = RemoteViews(context.packageName, R.layout.widget_clock)
        views.setTextViewText(R.id.widget_status, status)
        views.setOnClickPendingIntent(R.id.widget_clock_in, pendingAction(context, ACTION_CLOCK_IN))
        views.setOnClickPendingIntent(R.id.widget_clock_out, pendingAction(context, ACTION_CLOCK_OUT))
        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openApp)
        manager.updateAppWidget(id, views)
    }

    private fun pendingAction(context: Context, action: String): PendingIntent {
        val intent = Intent(context, ClockWidget::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
