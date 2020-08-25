package com.funglejunk.stockecho

import arrow.core.NonEmptyList
import arrow.core.Option
import arrow.core.Validated
import arrow.core.extensions.nonemptylist.foldable.get
import arrow.core.toOption
import arrow.fx.IO
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.model.PerformanceCalculation
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.repo.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

internal class PerformanceCalculationTest {

    @Test
    fun testCorrectReport() {
        val calculator = PerformanceCalculation()

        val prefs = prefsWith(
            listOf(
                Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
                Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
                Allocation("C", 3.0, BigDecimal.valueOf(10.0))
            )
        )

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0, "C" to 20.0
            )
        )

        val past = historyWith(
            listOf(
                "A" to 10.0, "B" to 10.0, "C" to 10.0
            )
        )

        val res = calculator.calculate(
            prefs,
            current,
            past
        ).unsafeRunSync()

        assertTrue(res is Validated.Valid)
        res as Validated.Valid
        val report = res.a
        assertEquals(60.0, report.absoluteToday, 0.001)
        assertEquals(100.0, report.perfToday, 0.001)
        assertEquals(100.0, report.perfTotal, 0.001)
        assertEquals(60.0, report.absoluteTotal, 0.001)
    }

    @Test
    fun testInvalidShares() {
        val calculator = PerformanceCalculation()

        val prefs = prefsWith(
            listOf(
                Allocation("A", 0.0, BigDecimal.valueOf(10.0)),
                Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
                Allocation("C", 3.0, BigDecimal.valueOf(10.0))
            )
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
            prefs,
            current,
            past
        ).unsafeRunSync()

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is PerformanceCalculation.CalculationError.ZeroShares)
    }

    @Test
    fun testMissingDataForToday() {
        val calculator = PerformanceCalculation()

        val prefs = prefsWith(
            listOf(
                Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
                Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
                Allocation("C", 3.0, BigDecimal.valueOf(10.0))
            )
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
            prefs,
            current,
            past
        ).unsafeRunSync()

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is PerformanceCalculation.CalculationError.IsinNotFoundInRemoteData)
    }

    @Test
    fun testMissingDataForYesterday() {
        val calculator = PerformanceCalculation()

        val prefs = prefsWith(
            listOf(
                Allocation("A", 1.0, BigDecimal.valueOf(10.0)),
                Allocation("B", 2.0, BigDecimal.valueOf(10.0)),
                Allocation("C", 3.0, BigDecimal.valueOf(10.0))
            )
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
            prefs,
            current,
            past
        ).unsafeRunSync()

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is PerformanceCalculation.CalculationError.IsinNotFoundInRemoteData)
    }

    @Test
    fun testFailingPrefsIO() {
        val calculator = PerformanceCalculation()

        val prefs = object : Prefs {
            override fun getAllocation(isin: String): IO<Option<Allocation>> {
                throw RuntimeException()
            }

            override fun getAllAllocations(): IO<List<Allocation>> = IO { emptyList() }
        }

        val current = historyWith(
            listOf(
                "A" to 20.0, "B" to 20.0, "C" to 20.0
            )
        )

        val past = historyWith(
            emptyList()
        )

        var errorThrown = false
        calculator.calculate(
            prefs,
            current,
            past
        ).attempt().unsafeRunSync().fold(
            { errorThrown = true },
            { }
        )

        assertTrue(errorThrown)
    }

    @Test
    fun testTooMuchData() {
        val calculator = PerformanceCalculation()

        val prefs = prefsWith(
            listOf(
                Allocation("A", 1.0, BigDecimal.valueOf(10.0))
            )
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
            prefs,
            current,
            past
        ).unsafeRunSync()

        assertTrue(res is Validated.Invalid)
        res as Validated.Invalid
        assertEquals(1, res.e.size)
        val error = res.e.getOrNull(0)
        assertTrue(error is PerformanceCalculation.CalculationError.TooManyCloseValues)
    }

    private fun prefsWith(allocations: List<Allocation>) = object : Prefs {
        override fun getAllocation(isin: String): IO<Option<Allocation>> = IO {
            allocations.find { it.isin == isin }.toOption()
        }

        override fun getAllAllocations(): IO<List<Allocation>> = IO { allocations }
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