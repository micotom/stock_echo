package com.funglejunk.stockecho.model

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.extensions.fx
import arrow.core.flatMap
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.toIO
import com.funglejunk.stockecho.data.*
import com.funglejunk.stockecho.getCurrentTradingDay
import com.funglejunk.stockecho.repo.SharedPrefs
import com.funglejunk.stockecho.verifySEOpen
import kotlinx.serialization.UnsafeSerializationApi
import timber.log.Timber

@UnsafeSerializationApi
class UpdateService : JobIntentService() {

    private companion object {
        fun getDataResultIntent(
            applicationContext: Context,
            report: Report,
            chartData: ChartData
        ): Intent =
            Intent(ACTION_REPORT_READY).apply {
                `package` = applicationContext.packageName
                putExtra(EXTRA_REPORT_KEY, report)
                putExtra(EXTRA_CHART_DATA_KEY, chartData.toTypedArray())
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
            val currentTradingDay = getCurrentTradingDay().verifySEOpen()
            val previousTradingDay = currentTradingDay.minusDays(1).verifySEOpen()
            val reportEither = !effect {
                interactor.calculatePerformance(currentTradingDay, previousTradingDay)
            }.bind()
            val chartDataEither = !effect {
                interactor.getChartData(currentTradingDay)
            }.bind()
            !reportEither.toIO() to !chartDataEither.toIO()
        }.flatMap {
            val dataResult = it.flatten()
            dataResult.fold(
                { errorList -> onError(errorList.asSimpleThrowable()) },
                { (report, chartData) -> onSuccess(report, chartData) }
            )
        }.attempt().unsafeRunAsync {
            it.fold(
                { e -> Timber.e("Could not deliver report: $e") },
                { Timber.d("update io successfully executed") }
            )
        }
    }

    private fun onError(t: Throwable): IO<Unit> = IO {
        sendBroadcast(getErrorIntent(applicationContext, t.message ?: "Unknown Error"))
    }

    private fun onSuccess(report: Report, chartData: ChartData): IO<Unit> = IO {
        sendBroadcast(getDataResultIntent(applicationContext, report, chartData))
    }

    private fun <T : Any> NonEmptyList<T>.asSimpleThrowable() = Throwable(
        map { it::class.java.simpleName }.toList().joinToString()
    )

    private fun <E, V1, V2> Pair<Validated<NonEmptyList<E>, V1>, Validated<NonEmptyList<E>, V2>>.flatten(): Validated<NonEmptyList<E>, Pair<V1, V2>> =
        first.fold(
            { e ->
                second.fold(
                    { Validated.Invalid(e.plus(it)) },
                    { Validated.Invalid(e) }
                )
            },
            { v1 ->
                second.fold(
                    { e -> Validated.Invalid(e) },
                    { v2 -> Validated.Valid(v1 to v2) }
                )
            }
        )

}