package com.funglejunk.stockecho.repo

import android.content.Context
import arrow.core.Either
import arrow.core.Option
import arrow.core.toOption
import arrow.fx.IO
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import java.math.BigDecimal

interface Prefs {
    fun getAllAllocations(): IO<Either<Throwable, List<Allocation>>>
}

@Serializable
data class Allocation(
    val isin: String,
    val nrOfShares: Double,
    @Serializable(with = BigDecimalSerializer::class) val buyPrice: BigDecimal
)

class MockPrefs : Prefs {

    private val mockAllocations = listOf(
        Allocation("LU1737652823", 8.0, BigDecimal.valueOf(61.99)),
        Allocation("IE00BKX55T58", 132.916, BigDecimal.valueOf(59.807)),
        Allocation("IE00BZ163L38", 35.559, BigDecimal.valueOf(46.3424)),
        Allocation("IE00B3VVMM84", 47.234, BigDecimal.valueOf(50.5778)),
        Allocation("IE00B1XNHC34", 68.0, BigDecimal.valueOf(7.4)),
        Allocation("IE00BK5BQV03", 37.0, BigDecimal.valueOf(55.58)),
        Allocation("IE00BK5BR733", 13.0, BigDecimal.valueOf(45.99))
    )

    override fun getAllAllocations() = IO { Either.right(mockAllocations) }

}

class SharedPrefs(applicationContext: Context) : Prefs {

    private val androidPrefs = applicationContext.getSharedPreferences(
        applicationContext.packageName, Context.MODE_PRIVATE
    )

    fun clear(): IO<Boolean> = IO {
        with(androidPrefs.edit()) {
            clear()
            commit()
        }
    }

    fun write(allocation: Allocation): IO<Boolean> = IO {
        Either.catch {
            with(androidPrefs.edit()) {
                putString(allocation.isin, Json.encodeToString(Allocation.serializer(), allocation))
                commit()
            }
        }.fold(
            { false },
            { it }
        )
    }

    override fun getAllAllocations(): IO<Either<Throwable, List<Allocation>>> = IO {
        getAllFromPrefs()
    }

    private suspend fun getAllFromPrefs(): Either<Throwable, List<Allocation>> = Either.catch {
        androidPrefs.all.map {
            @Suppress("UNCHECKED_CAST")
            it as Map.Entry<String, String>
            Json.decodeFromString(Allocation.serializer(), it.value)
        }
    }

}

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}