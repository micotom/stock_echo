package com.funglejunk.stockecho

import arrow.core.*
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.fx
import arrow.core.extensions.list.applicative.map
import arrow.core.extensions.list.traverse.sequence
import arrow.core.extensions.list.traverse.traverse
import arrow.core.extensions.listk.monad.map
import arrow.core.extensions.listk.traverse.sequence
import arrow.core.extensions.option.applicative.applicative
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.applicative.applicative
import arrow.fx.fix
import kotlinx.serialization.UnsafeSerializationApi
import java.time.LocalDate

typealias HistoryResponseIO = IO<Either<Throwable, Map<String, History>>>

@UnsafeSerializationApi
class UpdateServiceInteractor {

    private val prefs = MockPrefs()

    fun calculatePerformance(): IO<Either<Throwable, IO<Option<Report>>>> =
        IO.fx {
            val isins = prefs.getAllAllocations().bind().map { it.isin }.toTypedArray()
            val today = !effect {
                fetchDataForToday(*isins)
            }.bind()
            val yesterday = !effect {
                fetchDataForYesterday(*isins)
            }.bind()
            Either.fx {
                val todayData = !today
                val yesterdayData = !yesterday
                calculatePerformance(todayData, yesterdayData)
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

    private fun calculatePerformance(
        currentData: Map<String, History>,
        pastData: Map<String, History>
    ): IO<Option<Report>> =
        currentData.map { (isin, history) ->
            IO.fx {
                !(prefs.getAllocation(isin)).map { allocationOption ->
                    Option.fx {
                        val allocation = !allocationOption
                        val nrOfShares = !allocation.nrOfShares.toOption()
                        val todayClose = !history.data.firstOrNull()?.close.toOption() * nrOfShares
                        val yesterdayClose = !pastData[isin]?.data?.firstOrNull()?.close.toOption() * nrOfShares
                        val onBuy = (allocation.buyPrice * nrOfShares.bd()).toDouble()
                        Triple(todayClose, yesterdayClose, onBuy)
                    }
                }
            }
        }.sequence(IO.applicative()).fix()
            .map { it.sequence(Option.applicative()).fix() }
            .map { it.map { it.fix().toList() } }
            .map {
                it.map { valueList ->
                    valueList.fold(Triple(0.0, 0.0, 0.0)) { acc, new ->
                        val (accToday, accYesterday, accOnBuy) = acc
                        val (today, yesterday, onBuy) = new
                        Triple(accToday + today, accYesterday + yesterday, accOnBuy + onBuy)
                    }
                }
            }
            .map {
                it.map { (today, yesterday, onBuy) ->
                    val perfToday = today / (yesterday / 100.0) - 100.0
                    val absoluteToday = today - yesterday
                    val perfTotal = today / (onBuy / 100.0) - 100.0
                    val absoluteTotal = today - onBuy
                    (perfToday to absoluteToday) to (perfTotal to absoluteTotal)
                }.map {
                    val (today, total) = it
                    Report(
                        today.first.rounded(), // TODO rounding error not properly forwarded in INFINITY
                        today.second.rounded(),
                        total.first.rounded(),
                        total.second.rounded()
                    )
                }
            }

    private fun reqFromRepo(date: LocalDate, vararg isins: String): HistoryResponseIO =
        isins.toList().traverse(IO.applicative()) {
            Repo.getHistory(it, date, date)
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

    private fun Double.toOption(): Option<Double> = when (this) {
        0.0 -> None
        else -> Option.just(this)
    }

}