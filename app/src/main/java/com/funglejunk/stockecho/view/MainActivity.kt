package com.funglejunk.stockecho.view

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.funglejunk.stockecho.R
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val widgetId: Int by lazy {
        (intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID).also {
            if (it == AppWidgetManager.INVALID_APPWIDGET_ID) {
                Timber.e("invalid app widget id")
            }
        }
    }

    private val configurationIntent: Intent by lazy {
        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED, configurationIntent)

        setContentView(R.layout.activity_main)

        ok_button.setOnClickListener {
            finishIntent()
        }
    }

    private fun finishIntent() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        RemoteViews(packageName, R.layout.stock_echo_widget).also { views ->
            updateAppWidget(this, appWidgetManager, widgetId)
        }
        setResult(Activity.RESULT_OK, configurationIntent)
        finish()
    }

}