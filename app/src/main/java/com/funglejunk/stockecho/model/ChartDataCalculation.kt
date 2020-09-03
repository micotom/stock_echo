package com.funglejunk.stockecho.model

import arrow.core.NonEmptyList
import arrow.core.Validated
import arrow.core.extensions.list.applicative.map
import arrow.core.nel
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.wrap

class ChartDataCalculation {

    fun calculate(
        allocations: List<Allocation>,
        history: Map<String, History>
    ): Validated<NonEmptyList<CalculationError>, ChartData> = when(allocations.isNotEmpty()) {
        true -> when (history.isNotEmpty()) {
            true -> {
                val validatedCloses = history.values.map { historyEntry ->
                    historyEntry.data.map { historyEntryData ->
                        val nrOfSharesValidated = allocations.getNrOfShares(historyEntry.isin)
                        nrOfSharesValidated.map { nrOfShares ->
                            val close = historyEntryData.close * nrOfShares
                            historyEntryData.copy(close = close)
                        }
                    }.wrap()
                }.wrap()
                validatedCloses.map {
                    val data = it.flatten()
                    data.groupBy { it.date }.map {
                        it.key to it.value.fold(0.0) { acc, new -> acc + new.close }
                    }.sortedBy { it.first }.toList().map { it.second.toFloat() }
                }
            }
            false -> Validated.Invalid(CalculationError.ZeroRemoteCloseValue.nel())
        }
        false -> Validated.Invalid(CalculationError.EmptyPrefsData.nel())
    }

    private fun List<Allocation>.getNrOfShares(forIsin: String): Validated<NonEmptyList<CalculationError>, Double> = find {
        it.isin == forIsin
    }?.nrOfShares?.let {
        if (it != 0.0) {
            Validated.Valid(it)
        } else {
            Validated.Invalid(CalculationError.ZeroShares.nel())
        }
    } ?: {
        Validated.Invalid(CalculationError.EmptyPrefsData.nel())
    }()

}