package com.funglejunk.stockecho

import android.content.Intent
import arrow.core.Validated
import arrow.core.extensions.list.applicative.map
import java.math.BigDecimal
import java.math.RoundingMode

fun Double.bd(): BigDecimal = BigDecimal.valueOf(this)

fun Double.rounded(): Double = toBigDecimal().rounded()

fun BigDecimal.rounded(): Double = setScale(2, RoundingMode.HALF_UP).toDouble()

fun Double.percentString() = "$this%"

fun Double.euroString() = "$this€"

fun BigDecimal.isPercentFrom(other: BigDecimal): Double = ((this / other) * 100.0.bd()).toDouble()

fun Double.isPercentFrom(other: Double): Double = (this/other) * 100.0

fun Intent?.isAndroidWidgetIntent(): Boolean = this?.action in ANDROID_WIDGET_INTENTS

fun Intent?.validate(): Validated<Unit, Pair<Intent, String>> = this?.action?.let {
    Validated.Valid(Pair(this, it))
} ?: {
    Validated.Invalid(Unit)
}()

fun <E, T> List<Validated<E, T>>.wrap(): Validated<E, List<T>> = firstOrNull {
    it is Validated.Invalid
}?.let {
    Validated.Invalid((it as Validated.Invalid).e)
} ?: {
    Validated.Valid(this.map { (it as Validated.Valid).a })
}()