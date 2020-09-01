package com.funglejunk.stockecho

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.bd(): BigDecimal = BigDecimal.valueOf(this)

fun Double.rounded(): Double = toBigDecimal().rounded()

fun BigDecimal.rounded(): Double = setScale(2, RoundingMode.HALF_UP).toDouble()

fun Double.percentString() = "$this%"

fun Double.euroString() = "$this€"

fun BigDecimal.isPercentFrom(other: BigDecimal): Double = ((this / other) * 100.0.bd()).toDouble()

fun Double.isPercentFrom(other: Double): Double = (this/other) * 100.0