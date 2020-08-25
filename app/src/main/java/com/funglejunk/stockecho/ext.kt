package com.funglejunk.stockecho

import java.math.BigDecimal
import java.math.RoundingMode

fun Double.bd(): BigDecimal = BigDecimal.valueOf(this)

fun Double.rounded(): Double = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()