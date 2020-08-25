package com.funglejunk.stockecho


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class History(
    @SerialName("isin")
    val isin: String,
    @SerialName("data")
    val `data`: List<Data>,
    @SerialName("totalCount")
    val totalCount: Int,
    @SerialName("tradedInPercent")
    val tradedInPercent: Boolean
) {
    @Serializable
    data class Data(
        @SerialName("date")
        val date: String,
        @SerialName("open")
        val `open`: Double,
        @SerialName("close")
        val close: Double,
        @SerialName("high")
        val high: Double,
        @SerialName("low")
        val low: Double,
        @SerialName("turnoverPieces")
        val turnoverPieces: Int,
        @SerialName("turnoverEuro")
        val turnoverEuro: Double
    )
}