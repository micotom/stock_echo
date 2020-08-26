package com.funglejunk.stockecho.data

import com.funglejunk.stockecho.bd
import com.funglejunk.stockecho.rounded
import java.math.BigDecimal

inline class Euros(val amount: BigDecimal) {

    operator fun times(other: Double): Euros = Euros(amount * other.bd())

    operator fun plus(other: Euros): Euros = Euros(amount + other.amount)

    operator fun div(other: Double): Euros = this / other.bd()

    operator fun div(other: Euros): Euros = this / other.amount

    operator fun div(other: BigDecimal): Euros = Euros(amount / other)

    operator fun minus(other: Double) = this / other.bd()

    operator fun minus(other: Euros) = this / other.amount

    operator fun minus(other: BigDecimal) = Euros(amount - other)
    
}