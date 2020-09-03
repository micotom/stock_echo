package com.funglejunk.stockecho.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.fx
import arrow.core.extensions.list.applicative.map
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.listk.monad.map
import arrow.core.extensions.listk.traverse.sequence
import arrow.core.fix
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.extensions.io.concurrent.parTraverse
import arrow.fx.extensions.toIO
import arrow.fx.fix
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.data.Report
import com.funglejunk.stockecho.repo.Prefs
import com.funglejunk.stockecho.repo.RemoteRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.UnsafeSerializationApi
import java.time.LocalDate

typealias ChartData = List<Float>
typealias HistoryResponse = Either<Throwable, Map<String, History>>
typealias HistoryResponseIO = IO<HistoryResponse>
typealias RepoRequest = (LocalDate, LocalDate, Array<out String>) -> HistoryResponseIO

typealias InteractorResult<T> = IO<Either<Throwable, Validated<NonEmptyList<CalculationError>, T>>>

@UnsafeSerializationApi
class UpdateServiceInteractor(
    private val prefs: Prefs,
    private val repoRequest: RepoRequest = ::reqFromRepo
) {

    fun getChartData(lastDay: LocalDate): InteractorResult<ChartData> =
        IO.fx {
            val allocations = prefs.getAllAllocations().bind().toIO().bind()
            val isins = allocations.map { it.isin }.toTypedArray()
            val historyEither = repoRequest.invoke(
                lastDay.minusDays(100),
                lastDay,
                isins
            ).bind()
            Either.fx {
                val history = !historyEither
                ChartDataCalculation().calculate(allocations, history)
            }
        }

    fun calculatePerformance(
        currentTradingDay: LocalDate,
        previousTradingDay: LocalDate
    ): InteractorResult<Report> =
        IO.fx {
            val allocations = prefs.getAllAllocations().bind().toIO().bind()
            val isins = allocations.map { it.isin }.toTypedArray()
            val (todayEither, yesterdayEither) = IO.parMapN(
                Dispatchers.IO,
                !effect { reqFromRepoSingleDay(currentTradingDay, *isins) },
                !effect { reqFromRepoSingleDay(previousTradingDay, *isins) }
            ) { today, yesterday ->
                today to yesterday
            }.bind()
            Either.fx {
                val today = !todayEither
                val yesterday = !yesterdayEither
                PerformanceCalculation().calculate(allocations, today, yesterday)
            }
        }

    private fun reqFromRepoSingleDay(date: LocalDate, vararg isins: String): HistoryResponseIO =
        repoRequest.invoke(date, date, isins)

}

@UnsafeSerializationApi
private fun reqFromRepo(
    minDate: LocalDate,
    maxDate: LocalDate,
    vararg isins: String
): HistoryResponseIO =
    isins.toList().parTraverse {
        RemoteRepo.getHistory(it, minDate, maxDate)
    }.map {
        it.sequence(Either.applicative()).fix().map {
            it.map { history ->
                history.isin to history
            }.toMap()
        }
    }