package com.funglejunk.stockecho.view

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import arrow.fx.IO
import com.funglejunk.stockecho.*
import com.funglejunk.stockecho.data.*
import timber.log.Timber

class StockEchoWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate()")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive(): ${intent?.action}")
        super.onReceive(context, intent)

        if (ANDROID_WIDGET_INTENTS.any { it == intent?.action }) return

        context?.let { safeContext ->
            val views = RemoteViews(context.packageName, R.layout.stock_echo_widget)
            when (intent?.action) {
                ACTION_REQUEST_UPDATE -> signalUpdateHappening(views, safeContext)
                ACTION_REPORT_READY -> displayNewReport(views, intent, safeContext)
                ACTION_ERROR -> displayErrorHappened(views, intent, safeContext)
                else -> logUnresolvableIntent(intent)
            }.attempt().unsafeRunSync().fold(
                {
                    Timber.e("Error while processing intent (action: ${intent?.action}: $it")
                },
                {
                    Timber.d("intent successfully handled: ${intent?.action}")
                }
            )
        }
    }

    private fun logUnresolvableIntent(intent: Intent?) = IO {
        Timber.w("Received unresolvable intent with action: ${intent?.action}")
    }

    private fun displayErrorHappened(views: RemoteViews, intent: Intent, context: Context) = IO {
        views.setErrorViewsVisible()
        intent.getStringExtra(EXTRA_ERROR_MSG)?.let { message ->
            views.setTextViewText(R.id.error_text, message)
            invalidateViews(context, views)
        }
    }

    private fun displayNewReport(views: RemoteViews, intent: Intent, context: Context) = IO {
        views.setDataViewsVisible()
        intent.getParcelableExtra<Report>(EXTRA_REPORT_KEY)?.let { report ->
            views.setTextViewText(R.id.today_perf_text, report.perfToday.percentString())
            views.setTextViewText(R.id.today_absolute_text, report.absoluteToday.euroString())
            views.setTextViewText(R.id.total_perf_text, report.perfTotal.percentString())
            views.setTextViewText(R.id.total_absolute_text, report.absoluteTotal.euroString())
            invalidateViews(context, views)
        }
    }

    private fun signalUpdateHappening(views: RemoteViews, context: Context) = IO {
        views.setDataViewsVisible()
        views.setTextViewText(R.id.today_perf_text, "...")
        views.setTextViewText(R.id.today_absolute_text, "...")
        views.setTextViewText(R.id.total_perf_text, "...")
        views.setTextViewText(R.id.total_absolute_text, "...")
        invalidateViews(context, views)
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
    Timber.d("updateAppWidget(): $appWidgetId")
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
    setViewVisibility(R.id.today_text, View.GONE)
    setViewVisibility(R.id.total_text, View.GONE)
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
    setViewVisibility(R.id.today_text, View.VISIBLE)
    setViewVisibility(R.id.total_text, View.VISIBLE)
}