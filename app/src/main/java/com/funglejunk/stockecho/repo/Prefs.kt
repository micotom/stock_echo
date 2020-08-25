package com.funglejunk.stockecho.repo

import arrow.core.Option
import arrow.core.toOption
import arrow.fx.IO
import java.math.BigDecimal

interface Prefs {

    fun getAllocation(isin: String): IO<Option<Allocation>>

    fun getAllAllocations(): IO<List<Allocation>>
}

data class Allocation(val isin: String, val nrOfShares: Double, val buyPrice: BigDecimal)

class MockPrefs : Prefs {

    private val mockAllocations = listOf(
        Allocation("LU1737652823",8.0, BigDecimal.valueOf(61.99)),
        Allocation("IE00BKX55T58",132.916, BigDecimal.valueOf(59.807)),
        Allocation("IE00BZ163L38",35.559, BigDecimal.valueOf(46.3424)),
        Allocation("IE00B3VVMM84",47.234, BigDecimal.valueOf(50.5778)),
        Allocation("IE00B1XNHC34",68.0, BigDecimal.valueOf(7.4)),
        Allocation("IE00BK5BQV03",37.0, BigDecimal.valueOf(55.58)),
        Allocation("IE00BK5BR733",13.0, BigDecimal.valueOf(45.99))
    )

    override fun getAllocation(isin: String): IO<Option<Allocation>> =
        IO { mockAllocations.find { it.isin == isin }.toOption() }

    override fun getAllAllocations() = IO { mockAllocations }

}