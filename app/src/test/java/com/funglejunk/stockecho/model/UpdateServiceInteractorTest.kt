package com.funglejunk.stockecho.model

import arrow.core.Either
import arrow.core.Invalid
import arrow.core.NonEmptyList
import arrow.core.Valid
import arrow.core.extensions.validated.foldable.size
import arrow.fx.IO
import com.funglejunk.stockecho.bd
import com.funglejunk.stockecho.data.History
import com.funglejunk.stockecho.repo.Allocation
import com.funglejunk.stockecho.repo.Prefs
import kotlinx.serialization.UnsafeSerializationApi
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@UnsafeSerializationApi
internal class UpdateServiceInteractorTest {

    private val currentDay = LocalDate.parse("2020-09-11")
    private val yesterday = currentDay.minusDays(1)

    @Test
    fun `empty prefs, empty repo`() {
        val emptyPrefs = getPrefsWith(Either.right(emptyList()))
        val emptyRepoResponse = getRepoReqWith(Either.right(emptyMap()))
        val interactor = UpdateServiceInteractor(emptyPrefs, emptyRepoResponse)

        val data = interactor.getChartData(currentDay).unsafeRunSync()
        assertTrue(data is Either.Right)
        data as Either.Right
        assertTrue(data.b.isInvalid)
        val content = data.b as Invalid<NonEmptyList<CalculationError>>
        assertEquals(1, content.e.size)
        assertEquals(CalculationError.EmptyPrefsData, content.e.head)

        val perf = interactor.calculatePerformance(currentDay, yesterday).unsafeRunSync()
        assertTrue(perf is Either.Right)
        perf as Either.Right
        val validated = perf.b
        assertFalse(validated.isValid)
    }

    @Test
    fun `non empty prefs, non empty repo`() {
        val nrOfShares = 10.0
        val allocations = listOf(
            Allocation("A", nrOfShares, 20.0.bd())
        )
        val prefs = getPrefsWith(
            Either.right(allocations)
        )
        val closeValue = 100.0
        val repoResponse = getRepoReqWith(
            Either.right(
                allocations.map {
                    it.isin to getHistoryWith(
                        it.isin, listOf(getHistoryDataWith("2020-01-01", closeValue))
                    )
                }.toMap()
            )
        )
        val interactor = UpdateServiceInteractor(prefs, repoResponse)

        val data = interactor.getChartData(currentDay).unsafeRunSync()
        assertTrue(data is Either.Right)
        data as Either.Right
        assertTrue(data.b.isValid)
        val content = data.b as Valid
        assertTrue(content.a.isNotEmpty())
        assertEquals(1, content.a.size)
        assertEquals((closeValue * nrOfShares).toFloat(), content.a[0])

        val perf = interactor.calculatePerformance(currentDay, yesterday).unsafeRunSync()
        assertTrue(perf is Either.Right)
        perf as Either.Right
        val validated = perf.b
        assertTrue(validated.isValid)
    }

    @Test
    fun `prefs failing`() {
        val failingPrefs = object : Prefs {
            override fun getAllAllocations(): IO<Either<Throwable, List<Allocation>>> = IO {
                Either.catch { throw NullPointerException() }
            }
        }
        val emptyRepoResponse = getRepoReqWith(Either.right(emptyMap()))
        val interactor = UpdateServiceInteractor(failingPrefs, emptyRepoResponse)

        interactor.getChartData(currentDay).attempt().unsafeRunSync().fold(
            {
                assertTrue(it is NullPointerException)
            },
            {
                fail()
            }
        )
    }

    @Test
    fun `repo failing`() {
        val allocations = listOf(
            Allocation("A", 1.0, 20.0.bd())
        )
        val prefs = getPrefsWith(Either.right(allocations))
        val failingRepo: RepoRequest = { _, _, _ ->
            IO { Either.catch { throw SocketTimeoutException() }}
        }
        val interactor = UpdateServiceInteractor(prefs, failingRepo)

        interactor.getChartData(currentDay).attempt().unsafeRunSync()
            .fold(
            {
                fail()
            },
            {
                assertTrue(it is Either.Left)
                it as Either.Left
                assertTrue(it.a is SocketTimeoutException)
            }
        )
    }

    private fun getPrefsWith(
        result: Either<Throwable, List<Allocation>>
    ): Prefs = object : Prefs {
        override fun getAllAllocations(): IO<Either<Throwable, List<Allocation>>> = IO { result }
    }

    private fun getRepoReqWith(
        result: HistoryResponse
    ): RepoRequest = { _, _, _ -> IO.just(result) }

    private fun getHistoryWith(isin: String, data: List<History.Data>) =
        History(
            isin = isin,
            totalCount = -1,
            tradedInPercent = false,
            data = data
        )

    private fun getHistoryDataWith(date: String, close: Double) = History.Data(
        date = date,
        close = close,
        open = -1.0,
        high = -1.0,
        low = -1.0,
        turnoverEuro = -1.0,
        turnoverPieces = -1
    )

}