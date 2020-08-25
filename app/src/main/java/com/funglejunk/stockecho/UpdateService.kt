package com.funglejunk.stockecho

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import arrow.fx.IO
import arrow.fx.extensions.fx
import kotlinx.serialization.UnsafeSerializationApi
import timber.log.Timber

@UnsafeSerializationApi
class UpdateService : JobIntentService() {

    private companion object {
        fun getDataReportIntent(applicationContext: Context, report: Report): Intent =
            Intent(ACTION_REPORT_READY).apply {
                `package` = applicationContext.packageName
                putExtra(EXTRA_REPORT_KEY, report)
            }
        fun getErrorIntent(applicationContext: Context, message: String): Intent =
            Intent(ACTION_ERROR).apply {
                `package` = applicationContext.packageName
                putExtra(EXTRA_ERROR_MSG, message)
            }
    }

    private val interactor = UpdateServiceInteractor()

    override fun onHandleWork(intent: Intent) {
        IO.fx {
            val dataFetch = !effect {
                interactor.calculatePerformance()
            }.bind()
            dataFetch.fold(
                { onError(it) },
                { calculation ->
                    !effect {
                        calculation
                    }.bind().map {
                        it.fold(
                            { onError(Throwable("Calculation failed")) },
                            { report -> onSuccess(report) }
                        )
                    }
                }
            )
        }.unsafeRunSync()
    }

    private fun onError(t: Throwable) {
        Timber.e("Could not deliver report: $t")
        sendBroadcast(getErrorIntent(applicationContext, t.message ?: "Unknown Error"))
    }

    private fun onSuccess(report: Report) {
        sendBroadcast(getDataReportIntent(applicationContext, report))
    }

}