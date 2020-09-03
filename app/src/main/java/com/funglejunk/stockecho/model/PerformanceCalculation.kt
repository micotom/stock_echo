package com.funglejunk.stockecho.model

import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.Validated
import arrow.core.extensions.list.applicative.map
import arrow.core.nel
import com.funglejunk.stockecho.bd
import com.funglejunk.stockecho.data.Euros
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.data.Report
import com.funglejunk.stockecho.isPercentFrom
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.rounded
import com.funglejunk.stockecho.wrap
import java.math.BigDecimal

class PerformanceCalculation {

    private data class ValueValidationInfo(
        val nrOfShares: Validated<CalculationError, BigDecimal>,
        val todayClose: Validated<CalculationError, BigDecimal>,
        val yesterdayClose: Validated<CalculationError, BigDecimal>,
        val onBuy: Euros
    )

    private data class ValueInfo(
        val nrOfShares: BigDecimal,
        val todayClose: Euros,
        val yesterdayClose: Euros,
        val onBuy: Euros
    )

    private fun createReport(from: List<ValueInfo>): Report {
        val todayValue = from.fold(BigDecimal(0.0)) { acc, new ->
            acc + (new.nrOfShares * new.todayClose.amount)
        }
        val yesterdayValue = from.fold(BigDecimal(0.0)) { acc, new ->
            acc + (new.nrOfShares * new.yesterdayClose.amount)
        }
        val onBuyValue = from.fold(BigDecimal(0.0)) { acc, new ->
            acc + (new.nrOfShares * new.onBuy.amount)
        }
        val perfToday = todayValue.isPercentFrom(yesterdayValue) - 100.0
        val perfTotal = todayValue.isPercentFrom(onBuyValue) - 100.0
        val absToday = todayValue - yesterdayValue
        val absTotal = todayValue - onBuyValue
        return Report(
            perfToday.rounded(),
            absToday.rounded(),
            perfTotal.rounded(),
            absTotal.rounded()
        )
    }

    fun calculate(
        allocations: List<Allocation>,
        currentData: Map<String, History>,
        pastData: Map<String, History>
    ): Validated<NonEmptyList<CalculationError>, Report> =
        when (allocations.isEmpty()) {
            true -> Validated.Invalid(CalculationError.EmptyPrefsData.nel())
            false -> allocations.map { allocation ->
                val nrOfSharesValidated = allocation.nrOfShares.asValidatedDivisor()
                val todayValidated = currentData.getValueAsValidated(allocation.isin).closeValue
                val yesterdayValidated =
                    pastData.getValueAsValidated(allocation.isin).closeValue
                val onBuy = allocation.buyPrice
                ValueValidationInfo(
                    nrOfSharesValidated,
                    todayValidated,
                    yesterdayValidated,
                    Euros(onBuy)
                ).validate()
            }.wrap().map {
                createReport(it)
            }
        }

    private fun Double.asValidatedDivisor(): Validated<CalculationError, BigDecimal> =
        when (this) {
            0.0 -> Validated.Invalid(CalculationError.ZeroShares)
            else -> Validated.Valid(this.bd())
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

    private val Validated<CalculationError, History>.closeValue: Validated<CalculationError, BigDecimal>
        get() = fold(
            { Validated.Invalid(it) },
            {
                when (it.data.size) {
                    0 -> Validated.Invalid(CalculationError.NoRemoteCloseValue)
                    1 -> {
                        when (val value = it.data[0].close) {
                            0.0 -> Validated.Invalid(CalculationError.ZeroRemoteCloseValue)
                            else -> Validated.Valid(value.bd())
                        }
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
            ValueInfo(nrOfShares, Euros(todayClose), Euros(yesterdayClose), onBuy)
        }

}