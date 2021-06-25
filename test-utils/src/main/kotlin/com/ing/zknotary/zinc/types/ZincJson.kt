@file:Suppress("TooManyFunctions")
package com.ing.zknotary.zinc.types

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.CordaSerializers
import com.ing.zknotary.common.serialization.bfl.serializers.CordaX500NameSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSupportedAlgorithm
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.testing.resizeTo
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.testing.toSizedIntArray
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import net.corda.core.contracts.Amount
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.Issued
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.PartyAndReference
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.algorithm
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Date
import javax.security.auth.x500.X500Principal

public fun BigDecimal.toZincJson(integerSize: Int = 24, fractionSize: Int = 6): String = toJsonObject(integerSize, fractionSize).toString()
public inline fun <reified T : Any> Amount<T>.toZincJson(
    integerSize: Int = 24,
    fractionSize: Int = 6,
    tokenSize: Int = 8,
    serializersModule: SerializersModule = EmptySerializersModule
): String = toJsonObject(
    integerSize, fractionSize, tokenSize, serializersModule
).toString()
public fun String.toZincJson(size: Int): String = toJsonObject(size).toString()
public fun <T : Enum<T>> T.toZincJson(): String = toJsonObject().toString()
public fun ByteArray.toZincJson(serializedSize: Int): String = toJsonObject(serializedSize).toString()
public fun UniqueIdentifier.toZincJson(): String = toJsonObject().toString()
public fun LinearPointer<*>.toZincJson(): String = toJsonObject().toString()
public fun Instant.toZincJson(): String = toJsonObject().toString()
public fun Duration.toZincJson(): String = toJsonObject().toString()
public fun X500Principal.toZincJson(): String = toJsonObject().toString()
public fun Currency.toZincJson(): String = toJsonObject().toString()
public fun Date.toZincJson(): String = toJsonObject().toString()
public fun ZonedDateTime.toZincJson(): String = toJsonObject().toString()
public fun TimeWindow.toZincJson(): String = toJsonObject().toString()
public fun PrivacySalt.toZincJson(): String = toJsonObject().toString()
public fun SecureHash.toZincJson(): String = toJsonObject().toString()
public fun StateRef.toZincJson(): String = toJsonObject().toString()
public fun PublicKey.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun Party.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun AnonymousParty.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun AbstractParty.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun PartyAndReference.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun AttachmentConstraint.toZincJson(encodedSize: Int? = null): String =
    toJsonObject(encodedSize).toString()
public fun Issued<String>.toZincJson(encodedSize: Int): String =
    toJsonObject(encodedSize).toString()
public fun Collection<String>.toZincJson(collectionSize: Int, elementSize: Int): String =
    toJsonObject(collectionSize, elementSize).toString()
public fun Collection<Int>.toZincJson(collectionSize: Int): String =
    toJsonObject(collectionSize).toString()

public fun String?.toJsonObject(size: Int, isNullable: Boolean = false): JsonObject = buildJsonObject {
    val inner = buildJsonObject {
        put("chars", toSizedIntArray(size).toJsonArray())
        put("size", "${this@toJsonObject?.length ?: 0}")
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

public fun Collection<Int>.toJsonObject(collectionSize: Int): JsonObject = buildJsonObject {
    put("size", "$size")
    put("elements", this@toJsonObject.toIntArray().resizeTo(collectionSize).toJsonArray())
}

public fun Collection<String>.toJsonObject(collectionSize: Int, elementSize: Int): JsonObject = buildJsonObject {
    put("size", "$size")
    put(
        "elements",
        buildJsonArray {
            map {
                add(it.toJsonObject(elementSize))
            }
            repeat(collectionSize - size) {
                add((null as String?).toJsonObject(elementSize))
            }
        }
    )
}

public fun <T : Enum<T>> T.toJsonObject(): JsonPrimitive = JsonPrimitive(this.ordinal.toString())

public fun ByteArray.toJsonObject(serializedSize: Int): JsonObject = buildJsonObject {
    put("size", "$size")
    put("bytes", resizeTo(serializedSize).toJsonArray())
}

public fun BigDecimal.toJsonObject(integerSize: Int = 24, fractionSize: Int = 6): JsonObject = buildJsonObject {
    val stringRepresentation = toPlainString()
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

    put("sign", "${signum()}")
    put("integer", integer.toJsonArray())
    put("fraction", fraction.toJsonArray())
}

public fun Class<*>.sha256(): ByteArray = SecureHash.sha256(name).copyBytes()

public inline fun <reified T : Any> Amount<T>.toJsonObject(
    integerSize: Int = 24,
    fractionSize: Int = 6,
    tokenSize: Int = 8,
    serializersModule: SerializersModule = EmptySerializersModule
): JsonObject = buildJsonObject {
    val displayTokenSizeJson = displayTokenSize.toJsonObject(integerSize, fractionSize)
    val tokenTypeHashJson = token.javaClass.sha256().toJsonArray()
    val tokenJson = serialize(token, serializersModule = BFLSerializers + serializersModule).toJsonArray()

    tokenJson.size shouldBe tokenSize

    put("quantity", "$quantity")
    put("display_token_size", displayTokenSizeJson)
    put("token_type_hash", tokenTypeHashJson)
    put("token", tokenJson)
}

/* This method assumes that the mostSignificantBits of the id are 0 */
public fun UniqueIdentifier.toJsonObject(): JsonObject = buildJsonObject {
    // input validations
    id.mostSignificantBits shouldBe 0

    put("external_id", externalId.toJsonObject(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH, true))
    put("id", "${id.leastSignificantBits}")
}

public fun LinearPointer<*>.toJsonObject(): JsonObject = buildJsonObject {
    put("pointer", pointer.toJsonObject())
    put("class_name", type.name.toJsonObject(CordaSerializers.CLASS_NAME_SIZE))
    put("is_resolved", isResolved)
}

public fun Instant.toJsonObject(): JsonObject = buildJsonObject {
    put("seconds", "$epochSecond")
    put("nanos", "$nano")
}

public fun Duration.toJsonObject(): JsonObject = buildJsonObject {
    put("seconds", "$seconds")
    put("nanos", "$nano")
}

public fun X500Principal.toJsonObject(): JsonObject = buildJsonObject {
    put("name", name.toJsonObject(1024))
}

public fun Currency.toJsonObject(): JsonObject = buildJsonObject {
    put("code", currencyCode.toJsonObject(3))
}

public fun Date.toJsonObject(): JsonObject = buildJsonObject {
    put("millis", "$time")
}

public fun ZonedDateTime.toJsonObject(): JsonObject = buildJsonObject {
    val zoneIdHash = when (zone) {
        is ZoneOffset -> 0
        else -> zone.id.hashCode()
    }
    put("year", "$year")
    put("month", "$monthValue")
    put("day_of_month", "$dayOfMonth")
    put("hour", "$hour")
    put("minute", "$minute")
    put("second", "$second")
    put("nano_of_second", "$nano")
    put("zone_offset_seconds", "${offset.totalSeconds}")
    put("zone_id_hash", "$zoneIdHash")
}

public fun TimeWindow.toJsonObject(): JsonObject = buildJsonObject {
    val zero = buildJsonObject {
        put("seconds", "0")
        put("nanos", "0")
    }

    val fromTime = buildJsonObject {
        put("is_null", this@toJsonObject.fromTime == null)
        put("inner", this@toJsonObject.fromTime?.toJsonObject() ?: zero)
    }

    val untilTime = buildJsonObject {
        put("is_null", this@toJsonObject.untilTime == null)
        put("inner", this@toJsonObject.untilTime?.toJsonObject() ?: zero)
    }

    put("from_time", fromTime)
    put("until_time", untilTime)
}

public fun PrivacySalt.toJsonObject(): JsonObject = buildJsonObject {
    put("bytes", bytes.toJsonArray())
}

public fun SecureHash.toJsonObject(): JsonObject = buildJsonObject {
    put("algorithm", "${SecureHashSupportedAlgorithm.fromAlgorithm(algorithm).id}")
    put("bytes", bytes.toJsonObject(SecureHashSurrogate.BYTES_SIZE))
}

public fun StateRef.toJsonObject(): JsonObject = buildJsonObject {
    put("hash", txhash.toJsonObject())
    put("index", "$index")
}

public fun JsonObject.polymorphic(): JsonObject = buildJsonObject {
    put("value", this@polymorphic)
}

public fun JsonObject.optional(isNone: Boolean = false): JsonObject = buildJsonObject {
    put("is_none", isNone)
    put("inner", this@optional)
}

public fun PublicKey.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    val scheme = Crypto.findSignatureScheme(this@toJsonObject)
    if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
        put("scheme_id", "${scheme.schemeNumberID}")
    }

    put("encoded", encoded.toJsonObject(encodedSize))
}

@JvmName("CordaX500NameJsonObject")
public fun CordaX500Name.toJsonObject(): JsonObject = buildJsonObject {
    val name = serialize(this@toJsonObject, strategy = CordaX500NameSerializer)
    put("name", name.toJsonArray())
}

public fun AbstractParty.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    nameOrNull()?.let { put("name", it.toJsonObject()) }
    put("owning_key", owningKey.toJsonObject(encodedSize).polymorphic())
}

public fun AnonymousParty.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    put("owning_key", owningKey.toJsonObject(encodedSize).polymorphic())
}

public fun Party.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    put("name", name.toJsonObject())
    put("owning_key", owningKey.toJsonObject(encodedSize).polymorphic())
}

public fun PartyAndReference.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    put("party", party.toJsonObject(encodedSize).polymorphic())
    put("reference", reference.bytes.toJsonObject(PartyAndReferenceSurrogate.REFERENCE_SIZE))
}

public fun AttachmentConstraint.toJsonObject(encodedSize: Int? = null): JsonObject = when (this) {
    is HashAttachmentConstraint -> toJsonObject()
    is SignatureAttachmentConstraint -> toJsonObject(requireNotNull(encodedSize))
    else -> buildJsonObject {}
}

public fun HashAttachmentConstraint.toJsonObject(): JsonObject = buildJsonObject {
    put("attachment_id", attachmentId.toJsonObject())
}

public fun SignatureAttachmentConstraint.toJsonObject(encodedSize: Int): JsonObject = buildJsonObject {
    put("public_key", key.toJsonObject(encodedSize).polymorphic())
}

public fun Issued<String>.toJsonObject(encodedSize: Int, productStringSize: Int = 1): JsonObject = buildJsonObject {
    put("issuer", issuer.toJsonObject(encodedSize))
    put("product", product.toJsonObject(productStringSize))
}

public fun emptyPublicKey(encodedSize: Int, schemeId: Int? = null): JsonObject = buildJsonObject {
    schemeId?.let { put("scheme_id", "$it") }
    put("encoded", ByteArray(0).toJsonObject(encodedSize))
}

public fun emptyAnonymousParty(publicKeyEncodedSize: Int): JsonObject = buildJsonObject {
    put("owning_key", emptyPublicKey(publicKeyEncodedSize).polymorphic())
}

public fun Int?.toJsonObject(): JsonObject = buildJsonObject {
    val isNull = this@toJsonObject == null
    put("is_null", isNull)
    put("inner", "${if (isNull) 0 else this@toJsonObject}")
}
