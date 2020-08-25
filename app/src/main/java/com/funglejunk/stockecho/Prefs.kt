package com.funglejunk.stockecho

import arrow.core.Option
import arrow.core.toOption
import arrow.fx.IO
import java.math.BigDecimal

class Prefs {

    data class Allocation(val nrOfShares: Double, val buyPrice: BigDecimal)

    private val mockAllocations = mapOf(
        "LU1737652823" to Allocation(8.0, BigDecimal.valueOf(61.99)),
        "IE00BKX55T58" to Allocation(132.916, BigDecimal.valueOf(59.807)),
        "IE00BZ163L38" to Allocation(35.559, BigDecimal.valueOf(46.3424)),
        "IE00B3VVMM84" to Allocation(47.234, BigDecimal.valueOf(50.5778)),
        "IE00B1XNHC34" to Allocation(68.0, BigDecimal.valueOf(7.4)),
        "IE00BK5BQV03" to Allocation(37.0, BigDecimal.valueOf(55.58)),
        "IE00BK5BR733" to Allocation(13.0, BigDecimal.valueOf(45.99))
    )

    // TODO use Option
    fun getAllocation(isin: String): IO<Option<Allocation>> =
        IO { mockAllocations[isin].toOption() }

    fun getAllAllocations() = mockAllocations

}