package com.funglejunk.stockecho

import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.Validated
import arrow.core.extensions.list.applicative.map
import arrow.fx.IO
import arrow.fx.extensions.fx
import java.math.BigDecimal

class PerformanceCalculation {

    sealed class CalculationError {
        object IsinNotFoundInRemoteData : CalculationError()
        object ZeroShares : CalculationError()
        object TooManyCloseValues : CalculationError()
        object NoRemoteCloseValue : CalculationError()
    }

    private data class ValueValidationInfo(
        val nrOfShares: Validated<CalculationError, Double>,
        val todayClose: Validated<CalculationError, Double>,
        val yesterdayClose: Validated<CalculationError, Double>,
        val onBuy: BigDecimal
    )

    private data class ValueInfo(
        val nrOfShares: Double,
        val todayClose: Double,
        val yesterdayClose: Double,
        val onBuy: BigDecimal
    )

    fun calculate(
        prefs: Prefs,
        currentData: Map<String, History>,
        pastData: Map<String, History>
    ): IO<Validated<NonEmptyList<CalculationError>, Report>> =
        IO.fx {

            val allocations = prefs.getAllAllocations().bind()

            val info = allocations.map { allocation ->
                val nrOfSharesValidated = allocation.nrOfShares.asValidatedDivisor()
                val todayValidated = currentData.getValueAsValidated(allocation.isin).closeValue
                val yesterdayValidated =
                    pastData.getValueAsValidated(allocation.isin).closeValue
                val onBuy = allocation.buyPrice
                ValueValidationInfo(
                    nrOfSharesValidated,
                    todayValidated,
                    yesterdayValidated,
                    onBuy
                ).validate()
            }.wrap()

            val values = info.map { valueInfo ->
                valueInfo.map {
                    Triple(
                        it.nrOfShares * it.todayClose, it.nrOfShares * it.yesterdayClose,
                        it.nrOfShares * it.onBuy.toDouble()
                    )
                }
            }

            values.map {
                it.fold(Triple(0.0, 0.0, 0.0)) { acc, new ->
                    val (accToday, accYesterday, accOnBuy) = acc
                    val (today, yesterday, onBuy) = new
                    Triple(accToday + today, accYesterday + yesterday, accOnBuy + onBuy)
                }
            }.map { (today, yesterday, onBuy) ->
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

    private fun Double.asValidatedDivisor(): Validated<CalculationError, Double> =
        when (this) {
            0.0 -> Validated.Invalid(CalculationError.ZeroShares)
            else -> Validated.Valid(this)
        }

    private fun Map<String, History>.getValueAsValidated(key: String): Validated<CalculationError, History> =
        Option.fromNullable(this[key]).fold(
            {
                Validated.Invalid(CalculationError.IsinNotFoundInRemoteData)
            },
            {
                Validated.Valid(it)
            }
        )

    private val Validated<CalculationError, History>.closeValue: Validated<CalculationError, Double>
        get() = fold(
            { Validated.Invalid(it) },
            {
                when (it.data.size) {
                    0 -> Validated.Invalid(CalculationError.NoRemoteCloseValue)
                    1 -> {
                        Validated.Valid(it.data[0].close)
                    }
                    else -> Validated.Invalid(CalculationError.TooManyCloseValues)
                }
            }
        )

    private fun <E, V, R> parVal(
        v1: Validated<E, V>,
        v2: Validated<E, V>,
        v3: Validated<E, V>,
        f: (V, V, V) -> R
    ): Validated<NonEmptyList<E>, R> =
        when {
            v1 is Validated.Valid && v2 is Validated.Valid && v3 is Validated.Valid -> Validated.Valid(
                f(v1.a, v2.a, v3.a)
            )
            v1 is Validated.Invalid && v2 is Validated.Valid && v3 is Validated.Valid -> v1.toValidatedNel()
            v1 is Validated.Valid && v2 is Validated.Invalid && v3 is Validated.Valid -> v2.toValidatedNel()
            v1 is Validated.Valid && v2 is Validated.Valid && v3 is Validated.Invalid -> v3.toValidatedNel()
            v1 is Validated.Invalid && v2 is Validated.Invalid && v3 is Validated.Valid -> Validated.Invalid(
                NonEmptyList(v1.e, listOf(v2.e))
            )
            v1 is Validated.Invalid && v2 is Validated.Valid && v3 is Validated.Invalid -> Validated.Invalid(
                NonEmptyList(v1.e, listOf(v3.e))
            )
            v1 is Validated.Valid && v2 is Validated.Invalid && v3 is Validated.Invalid -> Validated.Invalid(
                NonEmptyList(v2.e, listOf(v3.e))
            )
            v1 is Validated.Invalid && v2 is Validated.Invalid && v3 is Validated.Invalid -> Validated.Invalid(
                NonEmptyList(v1.e, listOf(v2.e, v3.e))
            )
            else -> throw IllegalStateException("Not possible value")
        }

    private fun ValueValidationInfo.validate(): Validated<NonEmptyList<CalculationError>, ValueInfo> =
        parVal(
            nrOfShares, todayClose, yesterdayClose
        ) { nrOfShares, todayClose, yesterdayClose ->
            ValueInfo(nrOfShares, todayClose, yesterdayClose, onBuy)
        }

    private fun <E, T> List<Validated<E, T>>.wrap(): Validated<E, List<T>> = firstOrNull {
        it is Validated.Invalid
    }?.let {
        Validated.Invalid((it as Validated.Invalid).e)
    } ?: {
        Validated.Valid(this.map { (it as Validated.Valid).a })
    }()

}