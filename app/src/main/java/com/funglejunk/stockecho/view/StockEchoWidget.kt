package com.funglejunk.stockecho.view

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import arrow.core.Either
import arrow.fx.IO
import com.funglejunk.stockecho.*
import com.funglejunk.stockecho.data.*
import com.funglejunk.stockecho.model.WidgetProviderInteractor
import com.funglejunk.stockecho.model.WorkerService
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

    private fun selectCallback(intentAction: String): (Context, Intent, RemoteViews) -> IO<Unit> =
        when (intentAction) {
            ACTION_REQUEST_UPDATE -> updateRequestCallback
            ACTION_REPORT_READY -> reportReadyCallback
            ACTION_ERROR -> errorCallback
            else -> unresolvableIntent
        }

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive(): ${intent?.action}")
        super.onReceive(context, intent)

        context?.let { safeContext ->
            val action = when (intent.isAndroidWidgetIntent()) {
                true -> IO {
                    val remoteViews =
                        RemoteViews(safeContext.packageName, R.layout.stock_echo_widget)
                    remoteViews.refreshClickIntent(safeContext)
                }
                false -> intent.validate().fold(
                    { IO { Timber.w("invalid intent to handle: $intent") } },
                    { (safeIntent, action) ->
                        onReceive(safeContext, safeIntent, action)
                    }
                )
            }
            action.attempt().unsafeRunAsync { result ->
                result.fold(
                    { Timber.e("Error while processing intent ($it)") },
                    { Timber.d("intent successfully handled") }
                )

            }
        }
    }

    private fun onReceive(context: Context, intent: Intent, action: String): IO<Unit> {
        val callback = selectCallback(action).invoke(
            context,
            intent,
            RemoteViews(context.packageName, R.layout.stock_echo_widget)
        )
        return WidgetProviderInteractor(
            WorkerService.Impl(context)
        ).onNewIntent(action, callback)
    }

    private val updateRequestCallback: (Context, Intent, RemoteViews) -> IO<Unit> =
        { context, _, views ->
            IO {
                views.setOnClickPendingIntent(R.id.layout_root, getUpdateRequestedIntent(context))
            }.map {
                views.setDataViewsVisible()
            }.map {
                with(views) {
                    setTextViewText(R.id.today_perf_text, "...")
                    setTextViewText(R.id.today_absolute_text, "...")
                    setTextViewText(R.id.total_perf_text, "...")
                    setTextViewText(R.id.total_absolute_text, "...")
                }
            }.map {
                views.setImageViewBitmap(R.id.canvas_view, null)
            }.map {
                invalidateViews(context, views)
            }
        }

    private val reportReadyCallback: (Context, Intent, RemoteViews) -> IO<Unit> =
        { context, intent, views ->
            IO {
                views.setDataViewsVisible()
            }.map {
                intent.getParcelableExtra<Report>(EXTRA_REPORT_KEY)?.let { report ->
                    with(views) {
                        setTextViewText(R.id.today_perf_text, report.perfToday.percentString())
                        setTextViewText(R.id.today_absolute_text, report.absoluteToday.euroString())
                        setTextViewText(R.id.total_perf_text, report.perfTotal.percentString())
                        setTextViewText(R.id.total_absolute_text, report.absoluteTotal.euroString())
                    }
                }
            }.flatMap {
                intent.getSerializableExtra(EXTRA_CHART_DATA_KEY)?.let { chartData ->
                    @Suppress("UNCHECKED_CAST")
                    chartData as Array<Float>
                    with(ChartCanvas(context)) {
                        val bmp =
                            Bitmap.createBitmap(CHART_WIDTH, CHART_HEIGHT, Bitmap.Config.ARGB_8888)
                        setBitmap(bmp)
                        draw(chartData.toList(), CHART_WIDTH, CHART_HEIGHT).map {
                            bmp
                        }
                    }
                } ?: IO.raiseError<Bitmap>(NullPointerException("No extra chart data key"))
            }.map {
                views.setImageViewBitmap(R.id.canvas_view, it)
            }.map {
                invalidateViews(context, views)
            }
        }

    private val errorCallback: (Context, Intent, RemoteViews) -> IO<Unit> =
        { context, intent, views ->
            IO {
                views.setErrorViewsVisible()
            }.map {
                intent.getStringExtra(EXTRA_ERROR_MSG)?.let { message ->
                    views.setTextViewText(R.id.error_text, message)
                }
            }.map {
                invalidateViews(context, views)
            }
        }

    private val unresolvableIntent: (Context, Intent, RemoteViews) -> IO<Unit> =
        { _, intent, _ ->
            IO {
                Timber.w("Received unresolvable intent with action: ${intent.action}")
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
private fun RemoteViews.refreshClickIntent(context: Context) = kotlin.run {
    setOnClickPendingIntent(R.id.layout_root, getUpdateRequestedIntent(context))
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
            PendingIntent.FLAG_UPDATE_CURRENT
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

private const val CHART_WIDTH = 480
private const val CHART_HEIGHT = 480