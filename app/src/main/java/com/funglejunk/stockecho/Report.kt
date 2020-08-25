package com.funglejunk.stockecho

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Report(
    val perfToday: Double,
    val absoluteToday: Double,
    val perfTotal: Double,
    val absoluteTotal: Double
) : Parcelable

fun Report.displayString() =
    "${if (perfToday > 0) "+" else ""}$perfToday%" + " / " +
            "${if (absoluteToday > 0) "+" else ""}$absoluteToday€" + " / " +
            "${if (perfTotal > 0) "+" else ""}$perfTotal%" + " / " +
            "${if (absoluteTotal > 0) "+" else ""}$absoluteTotal€"

fun Double.percentString() = "$this%"

fun Double.euroString() = "$this€"