package com.funglejunk.stockecho.model

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.fx
import arrow.core.extensions.list.applicative.map
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.listk.monad.map
import arrow.core.extensions.listk.traverse.sequence
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.extensions.toIO
import arrow.fx.fix
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.data.Report
import com.funglejunk.stockecho.getCurrentTradingDay
import com.funglejunk.stockecho.repo.Prefs
import com.funglejunk.stockecho.repo.RemoteRepo
import com.funglejunk.stockecho.verifySEOpen
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.UnsafeSerializationApi
import java.time.LocalDate

typealias HistoryResponse = Either<Throwable, Map<String, History>>
typealias HistoryResponseIO = IO<HistoryResponse>

@UnsafeSerializationApi
class UpdateServiceInteractor(private val prefs: Prefs) {

    fun calculatePerformance(): IO<Either<Throwable, Validated<NonEmptyList<PerformanceCalculation.CalculationError>, Report>>> =
        IO.fx {
            val allocations = prefs.getAllAllocations().bind().toIO().bind()
            val isins = allocations.map { it.isin }.toTypedArray()
            val (todayEither, yesterdayEither) = IO.parMapN(
                Dispatchers.IO,
                !effect { fetchDataForToday(*isins) },
                !effect { fetchDataForYesterday(*isins) }
            ) { today, yesterday ->
                today to yesterday
            }.bind()
            Either.fx {
                val today = !todayEither
                val yesterday = !yesterdayEither
                PerformanceCalculation().calculate(allocations, today, yesterday)
            }
        }

    private fun fetchDataForToday(vararg isins: String): HistoryResponseIO =
        getCurrentTradingDay().verifySEOpen().run {
            reqFromRepo(this, *isins)
        }

    private fun fetchDataForYesterday(vararg isins: String): HistoryResponseIO =
        getCurrentTradingDay().minusDays(1).verifySEOpen().run {
            reqFromRepo(this, *isins)
        }

    private fun reqFromRepo(date: LocalDate, vararg isins: String): HistoryResponseIO =
        isins.toList().traverse(IO.applicative()) {
            RemoteRepo.getHistory(it, date, date)
        }.fix().map {
            it.sequence(Either.applicative()).fix().map {
                it.map { history ->
                    history.isin to history
                }
            }
        }.map {
            it.map { pairs ->
                pairs.toMap()
            }
        }

}