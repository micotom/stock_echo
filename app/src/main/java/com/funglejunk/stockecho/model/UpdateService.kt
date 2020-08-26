package com.funglejunk.stockecho.model

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockecho.data.*
import com.funglejunk.stockecho.repo.Prefs
import com.funglejunk.stockecho.repo.SharedPrefs
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

    private val interactor: UpdateServiceInteractor by lazy {
        UpdateServiceInteractor(SharedPrefs(applicationContext))
    }

    override fun onHandleWork(intent: Intent) {
        IO.fx {
            !effect {
                interactor.calculatePerformance()
            }.bind()
        }.attempt().unsafeRunSync().fold(
            { onError(it) },
            { eitherValidatedReport ->
                eitherValidatedReport.fold(
                    { error ->
                        onError(error)
                    },
                    { validatedReport ->
                        when (validatedReport) {
                            is Validated.Invalid -> onError(
                                Throwable(validatedReport.e.asSimpleThrowable())
                            )
                            is Validated.Valid -> onSuccess(validatedReport.a)
                        }
                    }
                )
            }
        )
    }

    private fun onError(t: Throwable) {
        Timber.e("Could not deliver report: $t")
        sendBroadcast(getErrorIntent(applicationContext, t.message ?: "Unknown Error"))
    }

    private fun onSuccess(report: Report) {
        sendBroadcast(getDataReportIntent(applicationContext, report))
    }

    private fun <T: Any> NonEmptyList<T>.asSimpleThrowable() = Throwable(
        map { it::class.java.simpleName }.toList().joinToString()
    )

}