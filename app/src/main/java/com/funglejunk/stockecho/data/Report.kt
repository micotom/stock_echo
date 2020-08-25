package com.funglejunk.stockecho.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Report(
    val perfToday: Double,
    val absoluteToday: Double,
    val perfTotal: Double,
    val absoluteTotal: Double
) : Parcelable