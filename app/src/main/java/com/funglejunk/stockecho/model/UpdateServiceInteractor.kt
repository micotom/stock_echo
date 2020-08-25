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
import arrow.fx.fix
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.data.Report
import com.funglejunk.stockecho.getCurrentTradingDay
import com.funglejunk.stockecho.repo.MockPrefs
import com.funglejunk.stockecho.repo.RemoteRepo
import com.funglejunk.stockecho.verifySEOpen
import kotlinx.serialization.UnsafeSerializationApi
import java.time.LocalDate

typealias HistoryResponseIO = IO<Either<Throwable, Map<String, History>>>

@UnsafeSerializationApi
class UpdateServiceInteractor {

    private val prefs = MockPrefs()

    fun calculatePerformance(): IO<Either<Throwable, Validated<NonEmptyList<PerformanceCalculation.CalculationError>, Report>>> =
        IO.fx {
            val isins = prefs.getAllAllocations().bind().map { it.isin }.toTypedArray()
            val todayEither = !effect {
                fetchDataForToday(*isins)
            }.bind()
            val yesterdayEither = !effect {
                fetchDataForYesterday(*isins)
            }.bind()
            Either.fx<Throwable, Pair<Map<String, History>, Map<String, History>>> {
                !todayEither to !yesterdayEither
            }.flatMap { (today, yesterday) ->
                PerformanceCalculation().calculate(prefs, today, yesterday).attempt().unsafeRunSync()
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