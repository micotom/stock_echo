package com.funglejunk.stockecho.repo

import arrow.core.Either
import arrow.core.extensions.either.monad.flatten
import arrow.fx.IO
import com.funglejunk.stockecho.data.History
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Parameters
import com.github.kittinunf.fuel.coroutines.awaitStringResult
import com.github.kittinunf.fuel.httpGet
import kotlinx.serialization.UnsafeSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.time.LocalDate

@UnsafeSerializationApi
object RemoteRepo {

    private const val BASE_URL = "https://api.boerse-frankfurt.de/data"
    private const val PRICE_HISTORY_EP = "/price_history"
    private const val PERFORMANCE_EP = "/performance"
    private const val ISIN_PARAM_ID = "isin"
    private const val MIN_DATE_ID = "minDate"
    private const val MAX_DATE_ID = "maxDate"
    private val OFFSET_PARAM = "offset" to 0
    private val LIMIT_PARAM = "limit" to 505
    private val MIC_PARAM = "mic" to "XETR"

    fun getHistory(
        isin: String,
        minDate: LocalDate,
        maxDate: LocalDate
    ): IO<Either<Throwable, History>> = IO {
        req(
            BASE_URL + PRICE_HISTORY_EP,
            listOf(
                OFFSET_PARAM,
                LIMIT_PARAM,
                MIC_PARAM,
                ISIN_PARAM_ID to isin,
                MIN_DATE_ID to minDate,
                MAX_DATE_ID to maxDate
            )
        )
    }

    private suspend inline fun <reified T : Any> req(
        url: String,
        params: Parameters? = null
    ): Either<Throwable, T> = Either.catch {
        val response = url.httpGet(params).awaitStringResult().catchable()
        response.deserialize<T>()
    }.flatten()

    private suspend inline fun <reified T : Any> String.deserialize(): Either<Throwable, T> =
        Either.catch {
            Json.decodeFromString(deserializer = T::class.serializer(), string = this)
        }.also {
            if (it.isLeft()) {
                Timber.e("Error deserializing to ${T::class.java}: $this")
            }
        }

    private fun com.github.kittinunf.result.Result<String, FuelError>.catchable(): String =
        fold(
            { it },
            {
                Timber.e("Error fetching url: $it")
                throw it
            }
        )

}