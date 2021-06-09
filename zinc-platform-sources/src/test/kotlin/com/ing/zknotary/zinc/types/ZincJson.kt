package com.ing.zknotary.zinc.types

import com.ing.serialization.bfl.api.reified.serialize
import com.ing.zknotary.common.serialization.bfl.corda.LinearPointerSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.CordaX500NameSerializer
import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSupportedAlgorithm
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.testing.resizeTo
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.testing.toSizedIntArray
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
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

fun BigDecimal.toZincJson(integerSize: Int = 24, fractionSize: Int = 6) = toJsonObject(integerSize, fractionSize).toString()
inline fun <reified T : Any> Amount<T>.toZincJson(
    integerSize: Int = 24,
    fractionSize: Int = 6,
    tokenSize: Int = 8,
    serializersModule: SerializersModule = EmptySerializersModule
) = toJsonObject(
    integerSize, fractionSize, tokenSize, serializersModule
).toString()
fun String.toZincJson(size: Int) = toJsonObject(size).toString()
fun ByteArray.toZincJson(serializedSize: Int) = toJsonObject(serializedSize).toString()
fun UniqueIdentifier.toZincJson() = toJsonObject().toString()
fun LinearPointer<*>.toZincJson() = toJsonObject().toString()
fun Instant.toZincJson() = toJsonObject().toString()
fun Duration.toZincJson() = toJsonObject().toString()
fun X500Principal.toZincJson() = toJsonObject().toString()
fun Currency.toZincJson() = toJsonObject().toString()
fun Date.toZincJson() = toJsonObject().toString()
fun ZonedDateTime.toZincJson() = toJsonObject().toString()
fun TimeWindow.toZincJson() = toJsonObject().toString()
fun PrivacySalt.toZincJson() = toJsonObject().toString()
fun SecureHash.toZincJson() = toJsonObject().toString()
fun StateRef.toZincJson() = toJsonObject().toString()
fun PublicKey.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()
fun Party.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()
fun AnonymousParty.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()
fun AbstractParty.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()
fun PartyAndReference.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()
fun AttachmentConstraint.toZincJson(encodedSize: Int? = null) =
    toJsonObject(encodedSize).toString()
fun Issued<String>.toZincJson(encodedSize: Int) =
    toJsonObject(encodedSize).toString()

@JvmName("toJsonObject")
fun String.toJsonObject(size: Int) = buildJsonObject {
    put("chars", toSizedIntArray(size).toJsonArray())
    put("size", "$length")
}

@JvmName("toNullableJsonObject")
fun String?.toJsonObject(size: Int) = buildJsonObject {
    val inner = buildJsonObject {
        put("chars", toSizedIntArray(size).toJsonArray())
        put("size", "${this@toJsonObject?.length ?: 0}")
    }

    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

fun ByteArray.toJsonObject(serializedSize: Int) = buildJsonObject {
    put("size", "$size")
    put("bytes", resizeTo(serializedSize).toJsonArray())
}

/* This method assumes that the mostSignificantBits of the id are 0 */
fun UniqueIdentifier.toJsonObject() = buildJsonObject {
    // input validations
    id.mostSignificantBits shouldBe 0

    put("external_id", externalId.toJsonObject(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH))
    put("id", "${id.leastSignificantBits}")
}

fun LinearPointer<*>.toJsonObject() = buildJsonObject {
    put("pointer", pointer.toJsonObject())
    put("class_name", type.name.toJsonObject(LinearPointerSurrogate.MAX_CLASS_NAME_SIZE))
    put("is_resolved", isResolved)
}

fun Instant.toJsonObject() = buildJsonObject {
    put("seconds", "$epochSecond")
    put("nanos", "$nano")
}

fun Duration.toJsonObject() = buildJsonObject {
    put("seconds", "$seconds")
    put("nanos", "$nano")
}

fun X500Principal.toJsonObject() = buildJsonObject {
    put("name", name.toJsonObject(1024))
}

fun Currency.toJsonObject() = buildJsonObject {
    put("code", currencyCode.toJsonObject(3))
}

fun Date.toJsonObject() = buildJsonObject {
    put("millis", "$time")
}

fun ZonedDateTime.toJsonObject() = buildJsonObject {
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

fun TimeWindow.toJsonObject() = buildJsonObject {
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

fun PrivacySalt.toJsonObject() = buildJsonObject {
    put("bytes", bytes.toJsonArray())
}

fun SecureHash.toJsonObject() = buildJsonObject {
    put("algorithm", "${SecureHashSupportedAlgorithm.fromAlgorithm(algorithm).id}")
    put("bytes", bytes.toJsonObject(SecureHashSurrogate.BYTES_SIZE))
}

fun StateRef.toJsonObject() = buildJsonObject {
    put("hash", txhash.toJsonObject())
    put("index", "$index")
}

fun PublicKey.toJsonObject(encodedSize: Int) = buildJsonObject {
    val scheme = Crypto.findSignatureScheme(this@toJsonObject)
    if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
        put("scheme_id", "${scheme.schemeNumberID}")
    }

    put("encoded", encoded.toJsonObject(encodedSize))
}

@JvmName("CordaX500NameJsonObject")
fun CordaX500Name.toJsonObject() = buildJsonObject {
    val name = serialize(this@toJsonObject, strategy = CordaX500NameSerializer)
    put("name", name.toJsonArray())
}

fun AbstractParty.toJsonObject(encodedSize: Int) = buildJsonObject {
    nameOrNull()?.let { put("name", it.toJsonObject()) }
    put("owning_key", owningKey.toJsonObject(encodedSize))
}

fun AnonymousParty.toJsonObject(encodedSize: Int) = buildJsonObject {
    put("owning_key", owningKey.toJsonObject(encodedSize))
}

fun Party.toJsonObject(encodedSize: Int) = buildJsonObject {
    put("name", name.toJsonObject())
    put("owning_key", owningKey.toJsonObject(encodedSize))
}

fun PartyAndReference.toJsonObject(encodedSize: Int) = buildJsonObject {
    put("party", party.toJsonObject(encodedSize))
    put("reference", reference.bytes.toJsonObject(PartyAndReferenceSurrogate.REFERENCE_SIZE))
}

fun AttachmentConstraint.toJsonObject(encodedSize: Int? = null) = when (this) {
    is HashAttachmentConstraint -> toJsonObject()
    is SignatureAttachmentConstraint -> toJsonObject(requireNotNull(encodedSize))
    else -> buildJsonObject {}
}

fun HashAttachmentConstraint.toJsonObject() = buildJsonObject {
    put("attachment_id", attachmentId.toJsonObject())
}

fun SignatureAttachmentConstraint.toJsonObject(encodedSize: Int) = buildJsonObject {
    put("public_key", key.toJsonObject(encodedSize))
}

fun Issued<String>.toJsonObject(encodedSize: Int, productStringSize: Int = 1) = buildJsonObject {
    put("issuer", issuer.toJsonObject(encodedSize))
    put("product", product.toJsonObject(productStringSize))
}
