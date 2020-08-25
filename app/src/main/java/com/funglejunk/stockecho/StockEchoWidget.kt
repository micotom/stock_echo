package com.funglejunk.stockecho

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import timber.log.Timber

class StockEchoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        context?.let {
            val views = RemoteViews(context.packageName, R.layout.stock_echo_widget)
            when (intent?.action) {
                ACTION_REQUEST_UPDATE -> {
                    views.setDataViewsVisible()
                    views.setTextViewText(R.id.today_perf_text, "...")
                    views.setTextViewText(R.id.today_absolute_text, "...")
                    views.setTextViewText(R.id.total_perf_text, "...")
                    views.setTextViewText(R.id.total_absolute_text, "...")
                    invalidateViews(context, views)
                }
                ACTION_REPORT_READY -> {
                    views.setDataViewsVisible()
                    intent.getParcelableExtra<Report>(EXTRA_REPORT_KEY)?.let { report ->
                        views.setTextViewText(R.id.today_perf_text, report.perfToday.percentString())
                        views.setTextViewText(R.id.today_absolute_text, report.absoluteToday.euroString())
                        views.setTextViewText(R.id.total_perf_text, report.perfTotal.percentString())
                        views.setTextViewText(R.id.total_absolute_text, report.absoluteTotal.euroString())
                        invalidateViews(context, views)
                    }
                }
                ACTION_ERROR -> {
                    views.setErrorViewsVisible()
                    intent.getStringExtra(EXTRA_ERROR_MSG)?.let { message ->
                        views.setTextViewText(R.id.error_text, message)
                    }
                }
                else -> Timber.w("Received unresolvable intent with action: ${intent?.action}")
            }
        }
    }

    override fun onEnabled(context: Context) = Unit

    override fun onDisabled(context: Context) = Unit

    private fun invalidateViews(context: Context, views: RemoteViews) {
        AppWidgetManager.getInstance(context).updateAppWidget(
            ComponentName(context, StockEchoWidget::class.java), views
        )
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.stock_echo_widget)
    views.setTextViewText(R.id.today_perf_text, "-")
    views.setTextViewText(R.id.today_absolute_text, "-")
    views.setTextViewText(R.id.total_perf_text, "-")
    views.setTextViewText(R.id.total_absolute_text, "-")

    views.setOnClickPendingIntent(
        R.id.layout_root,
        getUpdateRequestedIntent(context)
    )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun getUpdateRequestedIntent(context: Context) = PendingIntent.getBroadcast(
    context, 0xab, Intent(ACTION_REQUEST_UPDATE).apply {
        `package` = context.packageName
    }, 0
)

private fun RemoteViews.setErrorViewsVisible() = kotlin.run {
    setViewVisibility(R.id.today_perf_text, View.GONE)
    setViewVisibility(R.id.today_absolute_text, View.GONE)
    setViewVisibility(R.id.total_perf_text, View.GONE)
    setViewVisibility(R.id.total_absolute_text, View.GONE)
    setViewVisibility(R.id.error_text, View.VISIBLE)
}

private fun RemoteViews.setDataViewsVisible() = kotlin.run {
    setViewVisibility(R.id.error_text, View.GONE)
    setViewVisibility(R.id.today_perf_text, View.VISIBLE)
    setViewVisibility(R.id.today_absolute_text, View.VISIBLE)
    setViewVisibility(R.id.total_perf_text, View.VISIBLE)
    setViewVisibility(R.id.total_absolute_text, View.VISIBLE)
}