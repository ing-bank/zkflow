package com.ing.zknotary.common.zinc.types

import com.ing.dlt.zkkrypto.util.asUnsigned
import net.corda.core.contracts.Amount
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security

fun <T : Any, U : Any> toWitness(left: Amount<T>, right: Amount<U>): String = "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

fun toWitness(left: BigDecimal, right: BigDecimal): String = "{\"left\": ${left.toJSON()}, \"right\": ${right.toJSON()}}"

fun <T : Any> Amount<T>.toJSON(): String {
    Security.addProvider(BouncyCastleProvider())
    val messageDigest = MessageDigest.getInstance("SHA-256")
    messageDigest.update(this.token::class.java.toString().toByteArray())
    val tokenNameHash = messageDigest.digest()

    return "{\"quantity\": \"$quantity\", \"display_token_size\": ${displayTokenSize.toJSON()}, \"token_name_hash\": ${tokenNameHash.toPrettyJSONArray()}}"
}

fun BigDecimal.toJSON(): String {
    val stringRepresentation = this.toPlainString()
    val integerFractionTuple = stringRepresentation.removePrefix("-").split(".")

    val integer = IntArray(1024)
    val startingIdx = 1024 - integerFractionTuple[0].length
    integerFractionTuple[0].forEachIndexed { idx, char ->
        integer[startingIdx + idx] = Character.getNumericValue(char)
    }

    val fraction = IntArray(128)
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
