package com.ing.zknotary.common.zinc.types

import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.zkp.ZincZKService
import net.corda.core.contracts.Amount
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.Logger
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

fun <T : Any, U : Any> toWitness(left: Amount<T>, right: Amount<U>): String = "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

fun toWitness(left: BigDecimal, right: BigDecimal): String = "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

fun toBigWitness(left: BigDecimal, right: BigDecimal): String =
    "{\"left\": ${left.toJSON(100, 20)}, \"right\": ${right.toJSON(100, 20)}}"

fun <T : Any> Amount<T>.toJSON(): String {
    Security.addProvider(BouncyCastleProvider())
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(this.token::class.java.toString().toByteArray())
    val tokenNameHash = messageDigest.digest()

    return "{\"quantity\": \"$quantity\", \"display_token_size\": ${displayTokenSize.toJSON()}, \"token_name_hash\": ${tokenNameHash.toPrettyJSONArray()}}"
}

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

    return "{\"sign\": \"${this.signum()}\", \"integer\": ${integer.toPrettyJSONArray()}, \"fraction\": ${fraction.toPrettyJSONArray()}}"
}

private fun IntArray.toPrettyJSONArray() = "[ ${this.joinToString { "\"$it\"" }} ]"

private fun ByteArray.toPrettyJSONArray() = "[ ${this.map { it.asUnsigned() }.joinToString { "\"$it\"" }} ]"

fun makeBigDecimal(bytes: ByteArray, sign: Int) = BigDecimal(BigInteger(sign, bytes))

fun makeBigDecimal(string: String, scale: Int) = BigDecimal(BigInteger(string), scale)

@ExperimentalTime
fun ZincZKService.setupTimed(log: Logger) {
    val time = measureTime {
        this.setup()
    }
    log.debug("[setup] $time")
}

@ExperimentalTime
fun ZincZKService.proveTimed(witness: ByteArray, log: Logger): ByteArray {
    var proof: ByteArray
    val time = measureTime {
        proof = this.prove(witness)
    }
    log.debug("[prove] $time")
    return proof
}

@ExperimentalTime
fun ZincZKService.verifyTimed(proof: ByteArray, publicInput: ByteArray, log: Logger) {
    val time = measureTime {
        this.verify(proof, publicInput)
    }
    log.debug("[verify] $time")
}
