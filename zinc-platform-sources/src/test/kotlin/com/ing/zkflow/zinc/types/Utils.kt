package com.ing.zkflow.zinc.types

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zkflow.serialization.bfl.corda.AmountSerializer
import com.ing.zkflow.serialization.bfl.serializers.CordaSerializers
import com.ing.zkflow.testing.bytesToWitness
import com.ing.zkflow.testing.toJsonArray
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import net.corda.core.contracts.Amount
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Currency

inline fun <reified T : Any, reified U : Any> toWitness(left: Amount<T>, right: Amount<U>) =
    buildJsonObject {
        put("left", left.toJsonObject())
        put("right", right.toJsonObject())
    }.toString()

inline fun <reified T : Any, reified U : Any> toSerializedWitness(left: Amount<T>, right: Amount<U>) =
    buildJsonObject {
        put("left", left.toJsonArray())
        put("right", right.toJsonArray())
    }.toString()

fun toWitness(left: BigDecimal, right: BigDecimal) = buildJsonObject {
    put("left", left.toJsonObject())
    put("right", right.toJsonObject())
}.toString()

fun toSerializedWitness(left: BigDecimal, right: BigDecimal) = buildJsonObject {
    put("left", left.toJsonArray())
    put("right", right.toJsonArray())
}.toString()

fun toBigWitness(left: BigDecimal, right: BigDecimal) = buildJsonObject {
    put("left", left.toJsonObject(100, 20))
    put("right", right.toJsonObject(100, 20))
}.toString()

inline fun <reified T : Any> toWitness(item: T): String {
    val bytes = serialize(item, serializersModule = CordaSerializers.module)
    return bytesToWitness(bytes)
}

@Serializable
data class WrappedAmountString(
    val wrappedValue: @Contextual Amount<String>
)

@Serializable
data class WrappedAmountCurrency(
    val wrappedValue: @Contextual Amount<@Contextual Currency>
)

@Suppress("UNCHECKED_CAST") // If the token is of type, Amount is too
inline fun <reified T : Any> Amount<T>.toJsonArray() =
    when (this.token) {
        is String -> serialize(
            WrappedAmountString(this as Amount<String>), WrappedAmountString.serializer(),
            SerializersModule {
                contextual(AmountSerializer(SmallStringSerializer))
                contextual(SmallStringSerializer)
            }
        )
        is Currency -> serialize(
            WrappedAmountCurrency(this as Amount<Currency>), WrappedAmountCurrency.serializer(),
            SerializersModule {
                contextual(AmountSerializer(CurrencySerializer))
            }
        )
        else -> throw IllegalArgumentException("Amount<${this.token.javaClass.simpleName}> is not supported")
    }.toJsonArray()

@Serializable
private data class WrappedBigDecimal(
    @FixedLength([24, 6])
    val wrappedValue: @Contextual BigDecimal
)

private fun BigDecimal.wrap(): WrappedBigDecimal = WrappedBigDecimal(this)

fun BigDecimal.toJsonArray() = serialize(this.wrap(), serializersModule = BFLSerializers).toJsonArray()

fun makeBigDecimal(bytes: ByteArray, sign: Int) = BigDecimal(BigInteger(sign, bytes))

fun makeBigDecimal(string: String, scale: Int) = BigDecimal(BigInteger(string), scale)
