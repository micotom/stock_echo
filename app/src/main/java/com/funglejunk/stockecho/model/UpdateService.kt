package com.funglejunk.stockecho.model

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.fx.IO
import arrow.fx.extensions.fx
import com.funglejunk.stockecho.data.*
import com.funglejunk.stockecho.repo.SharedPrefs
import kotlinx.serialization.UnsafeSerializationApi
import timber.log.Timber

@UnsafeSerializationApi
class UpdateService : JobIntentService() {

    private companion object {
        fun getDataResultIntent(applicationContext: Context, report: Report, chartData: ChartData): Intent =
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
            val reportEither = !effect {
                interactor.calculatePerformance()
            }.bind()
            val chartDataEither = !effect {
                interactor.getChartData()
            }.bind()
            reportEither to chartDataEither
        }.unsafeRunAsync {
            val res = it.flattenToPair()
            res.fold(
                { error -> onError(Throwable(error::class.java.simpleName)) },
                { (validatedReport, chartData) ->
                    validatedReport.fold(
                        { errorList -> onError(errorList.asSimpleThrowable())},
                        { report -> onSuccess(report, chartData) }
                    )
                }
            )
        }
    }

    private fun onError(t: Throwable) {
        Timber.e("Could not deliver report: $t")
        sendBroadcast(getErrorIntent(applicationContext, t.message ?: "Unknown Error"))
    }

    private fun onSuccess(report: Report, chartData: ChartData) {
        sendBroadcast(getDataResultIntent(applicationContext, report, chartData))
    }

    private fun <T: Any> NonEmptyList<T>.asSimpleThrowable() = Throwable(
        map { it::class.java.simpleName }.toList().joinToString()
    )

    private fun <E1: Throwable, V1, E2: Throwable, V2> Either<Throwable, Pair<Either<E1, V1>, Either<E2, V2>>>.flattenToPair(): Either<Throwable, Pair<V1, V2>> =
        fold(
            {
                Either.left(it)
            },
            { (either1, either2) ->
                either1.fold(
                    {
                        Either.left(it)
                    },
                    { v1 ->
                        either2.fold(
                            {
                                Either.left(it)
                            },
                            { v2 ->
                                Either.right(v1 to v2)
                            }
                        )
                    }
                )
            }
        )

}