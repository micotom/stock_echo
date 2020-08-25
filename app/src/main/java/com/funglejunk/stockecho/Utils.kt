package com.funglejunk.stockecho

import java.lang.IllegalArgumentException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

fun getCurrentTradingDay(): LocalDate = LocalDateTime.now().run {
    when (this.hour < 9) {
        true -> LocalDate.now().minusDays(1)
        false -> LocalDate.now()
    }
}

fun LocalDate.verifySEOpen(): LocalDate =
    when (this.dayOfWeek) {
        DayOfWeek.SATURDAY -> minusDays(1).verifySEOpen()
        DayOfWeek.SUNDAY -> minusDays(2).verifySEOpen()
        else -> if (this.isGermanHoliday()) {
            minusDays(1).verifySEOpen()
        } else {
            this
        }
    }

private fun LocalDate.isGermanHoliday(): Boolean = when (this.year) {
    2020 -> this in holidays2020
    2021 -> this in holidays2021
    else -> throw IllegalArgumentException("No holidays matching ${this.year}")
}

private val holidays2020 = listOf(
    LocalDate.of(2020, 1, 1),
    LocalDate.of(2020, 4, 10),
    LocalDate.of(2020, 4, 13),
    LocalDate.of(2020, 5, 1),
    LocalDate.of(2020, 6, 1),
    LocalDate.of(2020, 10, 3),
    LocalDate.of(2020, 12, 25),
    LocalDate.of(2020, 12, 26)
)

private val holidays2021 = listOf(
    LocalDate.of(2021, 1, 1),
    LocalDate.of(2021, 5, 1),
    LocalDate.of(2021, 10, 3),
    LocalDate.of(2021, 12, 25),
    LocalDate.of(2021, 12, 26),
    LocalDate.of(2021, 4, 2),
    LocalDate.of(2021, 4, 4),
    LocalDate.of(2021, 4, 5),
    LocalDate.of(2021, 5, 13),
    LocalDate.of(2021, 5, 23),
    LocalDate.of(2021, 5, 24)
)