package com.funglejunk.stockecho

import arrow.core.*
import arrow.core.extensions.list.applicative.map
import arrow.core.extensions.nonemptylist.foldable.get
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.model.CalculationError
import com.funglejunk.stockecho.model.PerformanceCalculation
import com.funglejunk.stockecho.repo.Allocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

internal class PerformanceCalculationTest {

    @Test
    fun testCorrectReport() {
        val calculator = PerformanceCalculation()

        val onBuyAllocs = listOf(
            Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
            Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
            Allocation("C", 3.0, BigDecimal.valueOf(10.0))
        )
        val totalOnBuyValue =
            onBuyAllocs.sumByDouble { (it.buyPrice * it.nrOfShares.bd()).toDouble() }

        val currentAllocs = listOf("A" to 20.0, "B" to 20.0, "C" to 20.0)
        val current = historyWith(currentAllocs)
        val totalCurrentValue = currentAllocs.sumByDouble { (isin, close) ->
            close * (onBuyAllocs.find { it.isin == isin }!!.nrOfShares)
        }

        val pastAllocs = listOf("A" to 10.0, "B" to 10.0, "C" to 10.0)
        val past = historyWith(pastAllocs)
        val totalPastValue = pastAllocs.sumByDouble { (isin, close) ->
            close * (onBuyAllocs.find { it.isin == isin }!!.nrOfShares)
        }

        val res = calculator.calculate(
            onBuyAllocs,
            current,
            past
        )

        val expectedPerfToday = totalCurrentValue.isPercentFrom(totalPastValue) - 100.0
        val expectedAbsoluteToday = totalCurrentValue - totalPastValue
        val expectedPerfTotal = totalCurrentValue.isPercentFrom(totalOnBuyValue) - 100.0
        val expectedAbsoluteTotal = totalCurrentValue - totalOnBuyValue

        assertTrue(res is Validated.Valid)
        res as Validated.Valid
        val report = res.a
        assertEquals(expectedAbsoluteToday, report.absoluteToday, 0.001)
        assertEquals(expectedPerfToday, report.perfToday, 0.001)
        assertEquals(expectedPerfTotal, report.perfTotal, 0.001)
        assertEquals(expectedAbsoluteTotal, report.absoluteTotal, 0.001)
    }

    @Test
    fun testInvalidShares() {
        val calculator = PerformanceCalculation()

        val onBuyAllocs = listOf(
            Allocation("A", 0.0, BigDecimal.valueOf(10.0)),
            Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
            Allocation("C", 3.0, BigDecimal.valueOf(10.0))
        )

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0
            )
        )

        val past = historyWith(
            listOf(
                "A" to 10.0, "B" to 10.0, "C" to 10.0
            )
        )

        val res = calculator.calculate(
            onBuyAllocs,
            current,
            past
        )

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is CalculationError.ZeroShares)
    }

    @Test
    fun testMissingDataForToday() {
        val calculator = PerformanceCalculation()

        val onBuyAllocs = listOf(
            Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
            Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
            Allocation("C", 3.0, BigDecimal.valueOf(10.0))
        )

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0
            )
        )

        val past = historyWith(
            listOf(
                "A" to 10.0, "B" to 10.0, "C" to 10.0
            )
        )

        val res = calculator.calculate(
            onBuyAllocs,
            current,
            past
        )

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is CalculationError.IsinNotFoundInRemoteData)
    }

    @Test
    fun testMissingDataForYesterday() {
        val calculator = PerformanceCalculation()

        val onBuyAllocs = listOf(
            Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
            Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
            Allocation("C", 3.0, BigDecimal.valueOf(10.0))
        )

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0, "C" to 20.0
            )
        )

        val past = historyWith(
            emptyList()
        )

        val res = calculator.calculate(
            onBuyAllocs,
            current,
            past
        )

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is CalculationError.IsinNotFoundInRemoteData)
    }

    @Test
    fun testFailingPrefsIOonEmptyAllocs() {
        val calculator = PerformanceCalculation()

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0, "C" to 20.0
            )
        )

        val pastAllocs = emptyList<Pair<String, Double>>()
        val past = historyWith(pastAllocs)

        val result = calculator.calculate(
            emptyList(),
            current,
            past
        )

        assertTrue(result is Validated.Invalid)
    }

    @Test
    fun testFailingPrefsIOonWrongIsin() {
        val calculator = PerformanceCalculation()

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0, "C" to 20.0
            )
        )

        val pastAllocs = emptyList<Pair<String, Double>>()
        val past = historyWith(pastAllocs)

        val result = calculator.calculate(
            listOf(Allocation(isin = "D", nrOfShares = 0.0, buyPrice = 0.0.bd())),
            current,
            past
        )

        assertTrue(result is Validated.Invalid)
    }

    @Test
    fun testTooMuchData() {
        val calculator = PerformanceCalculation()

        val onBuyAllocs = listOf(
            Allocation("A", 1.0, BigDecimal.valueOf(10.0))
        )

        val current = mapOf(
            "A" to History(
                "A",
                listOf(
                    History.Data("", 0.0, 0.0, 0.0, 0.0, 0, 0.0),
                    History.Data("", 0.0, 0.0, 0.0, 0.0, 0, 0.0)
                ),
                0, false,
            )
        )

        val past = historyWith(
            listOf(
                "A" to 10.0
            )
        )

        val res = calculator.calculate(
            onBuyAllocs,
            current,
            past
        )

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is CalculationError.TooManyCloseValues)
    }

    private fun historyWith(values: List<Pair<String, Double>>) = values.map { (isin, close) ->
        isin to History(
            isin,
            listOf(History.Data("", 0.0, close, 0.0, 0.0, 0, 0.0)),
            0, false,
        )
    }.toMap()

    private fun <T> NonEmptyList<T>.getOrNull(index: Long): T? = get(index).fold(
        { null },
        { it }
    )

}