package zinc.types

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.zknotary.common.serialization.bfl.corda.AmountSerializer
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZincZKService
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.util.Currency
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

inline fun <reified T : Any, reified U : Any> toWitness(left: Amount<T>, right: Amount<U>): String =
    "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

inline fun <reified T : Any, reified U : Any> toSerializedWitness(left: Amount<T>, right: Amount<U>): String =
    "{\"left\": ${left.toSerializedJson()}, \"right\": ${right.toSerializedJson()}}"

fun toWitness(left: BigDecimal, right: BigDecimal): String =
    "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

fun toSerializedWitness(left: BigDecimal, right: BigDecimal): String =
    "{\"left\": ${left.toSerializedJson()}, \"right\": ${right.toSerializedJson()}}"

fun toBigWitness(left: BigDecimal, right: BigDecimal): String =
    "{\"left\": ${left.toJSON(100, 20)}, \"right\": ${right.toJSON(100, 20)}}"

fun Class<*>.sha256(): ByteArray = SecureHash.sha256(name).copyBytes()

@Serializable
data class WrappedAmountString(
    val wrappedValue: @Contextual Amount<String>
)

@Serializable
data class WrappedAmountCurrency(
    val wrappedValue: @Contextual Amount<@Contextual Currency>
)

inline fun <reified T : Any> Amount<T>.toSerializedJson(): String {
    return when (this.token) {
        is String -> serialize(
            WrappedAmountString(this as Amount<String>), WrappedAmountString.serializer(),
            SerializersModule {
                contextual(AmountSerializer(SmallStringSerializer))
            }
        )
        is Currency -> serialize(
            WrappedAmountCurrency(this as Amount<Currency>), WrappedAmountCurrency.serializer(),
            SerializersModule {
                contextual(AmountSerializer(CurrencySerializer))
            }
        )
        else -> throw IllegalArgumentException("Amount<${this.token.javaClass.simpleName}> is not supported")
    }.toPrettyJSONArray()
}

inline fun <reified T : Any> Amount<T>.toJSON(
    integerSize: Int = 24,
    fractionSize: Int = 6,
    serializersModule: SerializersModule = EmptySerializersModule
): String {
    val displayTokenSizeJson = this.displayTokenSize.toJSON(integerSize, fractionSize)
    val tokenTypeHashJson = this.token.javaClass.sha256().toPrettyJSONArray()
    val token = serialize(this.token, serializersModule = BFLSerializers + serializersModule)

    token.size shouldBe 8

    val tokenJson = token.toPrettyJSONArray()

    return "{\"quantity\": \"$quantity\", \"display_token_size\": $displayTokenSizeJson," +
        " \"token_type_hash\": $tokenTypeHashJson, \"token\": $tokenJson}"
}

@Serializable
private data class WrappedBigDecimal(
    @FixedLength([24, 6])
    val wrappedValue: @Contextual BigDecimal
)

private fun BigDecimal.wrap(): WrappedBigDecimal = WrappedBigDecimal(this)

fun BigDecimal.toSerializedJson(): String =
    serialize(this.wrap(), serializersModule = BFLSerializers).toPrettyJSONArray()

fun BigDecimal.toJSON(integerSize: Int = 24, fractionSize: Int = 6): String {
    val stringRepresentation = this.toPlainString()
    val integerFractionTuple = stringRepresentation.removePrefix("-").split(".")

    val integer = IntArray(integerSize)
    val startingIdx = integerSize - integerFractionTuple[0].length
    integerFractionTuple[0].forEachIndexed { idx, char ->
        integer[startingIdx + idx] = Character.getNumericValue(char)
    }

    val fraction = IntArray(fractionSize)
    if (integerFractionTuple.size == 2) {
        integerFractionTuple[1].forEachIndexed { idx, char ->
            fraction[idx] = Character.getNumericValue(char)
        }
    }

    return "{\"sign\": \"${this.signum()}\", \"integer\": ${integer.toPrettyJSONArray()}," +
        " \"fraction\": ${fraction.toPrettyJSONArray()}}"
}

private fun IntArray.toPrettyJSONArray() = "[ ${this.joinToString { "\"$it\"" }} ]"

internal fun ByteArray.toPrettyJSONArray() = "[ ${this.map { it.asUnsigned() }.joinToString { "\"$it\"" }} ]"

fun makeBigDecimal(bytes: ByteArray, sign: Int) = BigDecimal(BigInteger(sign, bytes))

fun makeBigDecimal(string: String, scale: Int) = BigDecimal(BigInteger(string), scale)

inline fun <reified T : Any> getZincZKService(
    buildTimeout: Duration = Duration.ofSeconds(5),
    setupTimeout: Duration = Duration.ofSeconds(300),
    provingTimeout: Duration = Duration.ofSeconds(300),
    verificationTimeout: Duration = Duration.ofSeconds(1)
): ZincZKService {
    val circuitFolder: String = T::class.java.getResource("/${T::class.java.simpleName}")!!.path
    return ZincZKService(
        circuitFolder,
        artifactFolder = circuitFolder,
        buildTimeout = buildTimeout,
        setupTimeout = setupTimeout,
        provingTimeout = provingTimeout,
        verificationTimeout = verificationTimeout,
    )
}

@ExperimentalTime
fun ZincZKService.setupTimed(log: Logger) {
    val time = measureTime {
        this.setup()
    }
    log.debug("[setup] $time")
}

@ExperimentalTime
fun ZincZKService.proveTimed(witness: Witness, log: Logger): ByteArray {
    var proof: ByteArray
    val time = measureTime {
        proof = this.prove(witness)
    }
    log.debug("[prove] $time")
    return proof
}

@ExperimentalTime
fun ZincZKService.proveTimed(witnessJson: String, log: Logger): ByteArray {
    var proof: ByteArray
    val time = measureTime {
        proof = this.prove(witnessJson)
    }
    log.debug("[prove] $time")
    return proof
}

@ExperimentalTime
fun ZincZKService.verifyTimed(proof: ByteArray, publicInputJson: String, log: Logger) {
    val time = measureTime {
        this.verify(proof, publicInputJson)
    }
    log.debug("[verify] $time")
}

@ExperimentalTime
fun ZincZKService.verifyTimed(proof: ByteArray, publicInput: PublicInput, log: Logger) {
    val time = measureTime {
        this.verify(proof, publicInput)
    }
    log.debug("[verify] $time")
}
