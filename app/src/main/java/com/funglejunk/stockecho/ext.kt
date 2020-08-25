package com.funglejunk.stockecho

import java.math.RoundingMode

fun Double.rounded(): Double = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()

fun Double.percentString() = "$this%"

fun Double.euroString() = "$this€"