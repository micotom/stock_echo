package com.funglejunk.stockecho.view

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.JobIntentService
import arrow.fx.IO
import com.funglejunk.stockecho.*
import com.funglejunk.stockecho.data.*
import com.funglejunk.stockecho.model.UpdateService
import kotlinx.serialization.UnsafeSerializationApi
import timber.log.Timber

@UnsafeSerializationApi
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

        if (intent?.action in ANDROID_WIDGET_INTENTS) return

        context?.let { _ ->
            val views = RemoteViews(context.packageName, R.layout.stock_echo_widget)
            val action = when (intent?.action) {
                ACTION_REQUEST_UPDATE -> processUpdateRequest(intent, views, context)
                ACTION_REPORT_READY -> displayNewReport(views, intent, context)
                ACTION_ERROR -> displayErrorHappened(views, intent, context)
                else -> logUnresolvableIntent(intent)
            }
            action.attempt().unsafeRunSync().fold(
                {
                    Timber.e("Error while processing intent (action: ${intent?.action}: $it")
                },
                {
                    Timber.d("intent successfully handled: ${intent?.action}")
                }
            )
        }
    }

    private fun processUpdateRequest(
        intent: Intent,
        views: RemoteViews,
        context: Context
    ): IO<Unit> {
        JobIntentService.enqueueWork(
            context, UpdateService::class.java, 0x42, intent
        )
        views.setOnClickPendingIntent(
            R.id.layout_root,
            getUpdateRequestedIntent(context)
        )
        return signalUpdateHappening(views, context)
    }

    private fun logUnresolvableIntent(intent: Intent?) = IO {
        Timber.w("Received unresolvable intent with action: ${intent?.action}")
    }

    private fun displayErrorHappened(views: RemoteViews, intent: Intent, context: Context) = IO {
        views.setErrorViewsVisible()
        intent.getStringExtra(EXTRA_ERROR_MSG)?.let { message ->
            with(views) {
                setTextViewText(R.id.error_text, message)
                invalidateViews(context, this)
            }
        }
    }

    private fun displayNewReport(views: RemoteViews, intent: Intent, context: Context) = IO {
        views.setDataViewsVisible()
        intent.getParcelableExtra<Report>(EXTRA_REPORT_KEY)?.let { report ->
            with(views) {
                setTextViewText(R.id.today_perf_text, report.perfToday.percentString())
                setTextViewText(R.id.today_absolute_text, report.absoluteToday.euroString())
                setTextViewText(R.id.total_perf_text, report.perfTotal.percentString())
                setTextViewText(R.id.total_absolute_text, report.absoluteTotal.euroString())
                invalidateViews(context, this)
            }
        }
    }

    private fun signalUpdateHappening(views: RemoteViews, context: Context) = IO {
        with(views) {
            setDataViewsVisible()
            setTextViewText(R.id.today_perf_text, "...")
            setTextViewText(R.id.today_absolute_text, "...")
            setTextViewText(R.id.total_perf_text, "...")
            setTextViewText(R.id.total_absolute_text, "...")
            invalidateViews(context, this)
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

@UnsafeSerializationApi
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    Timber.d("updateAppWidget(): $appWidgetId")
    val views = RemoteViews(context.packageName, R.layout.stock_echo_widget)
    with(views) {
        setTextViewText(R.id.today_perf_text, "-")
        setTextViewText(R.id.today_absolute_text, "-")
        setTextViewText(R.id.total_perf_text, "-")
        setTextViewText(R.id.total_absolute_text, "-")

        setOnClickPendingIntent(
            R.id.layout_root,
            getUpdateRequestedIntent(context)
        )

        appWidgetManager.updateAppWidget(appWidgetId, this)
    }
}

@UnsafeSerializationApi
private fun getUpdateRequestedIntent(context: Context): PendingIntent =
    with(
        Intent(context, StockEchoWidget::class.java).apply {
            action = ACTION_REQUEST_UPDATE
        }
    ) {
        PendingIntent.getBroadcast(
            context,
            PI_REQUEST_UPDATE_ID,
            this,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

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