package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaX500NameSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.resizeTo
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.zinc.types.polymorphic
import com.ing.zknotary.zinc.types.toJsonObject
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.serialization.NetworkSurrogate
import io.ivno.collateraltoken.serialization.PermissionSurrogate
import io.ivno.collateraltoken.serialization.RoleSurrogate
import io.ivno.collateraltoken.serialization.SettingSurrogate
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Permission
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.bnms.contract.Setting
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.contracts.LinearPointer
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

fun Role.toZincJson() = toJsonObject().toString()
fun TokenDescriptor.toZincJson() = toJsonObject().toString()
fun Permission.toZincJson() = toJsonObject().toString()
fun BigDecimalAmount<LinearPointer<IvnoTokenType>>.toZincJson() = toJsonObject().toString()
fun Network.toZincJson(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) =
    toJsonObject(encodedSize, isAnonymous, scheme).toString()

fun Setting<String>.toZincJson(size: Int) = toJsonObject(size).toString()

fun JsonObject.polymorphic() = buildJsonObject {
    put("value", this@polymorphic)
}

fun Redemption.toZincJson() = toJsonObject().toString()

/**
 * Extension function for encoding a nullable ByteArray to Json
 * @param serializedSize The size of the byte array
 * @param isEmpty Boolean flag indicating whether the nullability wrapper will be applied on the Json structure
 *
 * @return the JsonObject of the byte array
 */
fun ByteArray?.toJsonObject(serializedSize: Int, isEmpty: Boolean = true) = buildJsonObject {
    val inner = buildJsonObject {
        put("size", "${this@toJsonObject?.size ?: 0}")
        put("bytes", resizeTo(serializedSize).toJsonArray())
    }

    if (isEmpty) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

/**
 * Extension function for encoding a nullable PublicKey to Json
 * @param encodedSize The size of the inner byte array
 * @param scheme The signature scheme of the key (useful in the case of a null key)
 * @param isEmpty Boolean flag indicating whether the nullability wrapper will be applied on the Json structure
 *
 * @return the JsonObject of the public key
 */
fun PublicKey?.toJsonObject(encodedSize: Int, scheme: SignatureScheme, isEmpty: Boolean = true) = buildJsonObject {
    // In the null case there is no way to know the intended implementation class. Given the fact that there are various
    // ways of serializing empty PublicKeys depending on their scheme we need to know somehow which empty version to
    // encode to Json. Thus, the 'scheme' is explicitly passed in this function.
    val inner = buildJsonObject {
        if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
            put("scheme_id", "${this@toJsonObject?.let { scheme.schemeNumberID } ?: 0}")
        }

        put(
            key = "encoded",
            element = this@toJsonObject?.let { it.encoded.toJsonObject(encodedSize) }
                ?: ByteArray?::toJsonObject.invoke(null, encodedSize, true)
        )
    }

    if (isEmpty) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner.polymorphic())
}

/**
 * Extension function for encoding a nullable AbstractParty to Json
 * @param encodedSize The size of the inner byte array of the owning key
 * @param isAnonymous Boolean flag indicating the implementation class of the abstract party (useful in the case of a
 * null abstract party)
 * @param scheme The signature scheme of the owning key (useful in the case of a null abstract party)
 *
 * @return the JsonObject of the abstract party
 */
fun AbstractParty?.toJsonObject(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) = buildJsonObject {
    // In the null case there is no way to know the intended implementation class. Given the fact that the empty
    // serialization of AnonymousParty differs from the one for Party (due to the CordaX500Name name property),
    // we need to know somehow which empty version to encode to Json. Thus, the 'isAnonymous' flag is used.
    val inner = buildJsonObject {
        if (!isAnonymous) {
            put(
                key = "name",
                element = this@toJsonObject?.let { nameOrNull()?.toJsonObject() }
                    ?: ByteArray(CordaX500NameSurrogate.SIZE) { 0 }.toJsonArray()
            )
        }
        put(
            key = "owning_key",
            element = (this@toJsonObject?.let {
                owningKey.toJsonObject(
                    encodedSize,
                    Crypto.findSignatureScheme(owningKey)
                )
            }
                ?: PublicKey?::toJsonObject.invoke(null, encodedSize, scheme, true)).polymorphic()
        )
    }

    put("is_null", this@toJsonObject == null)
    put("inner", inner.polymorphic())
}

fun Role.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(RoleSurrogate.VALUE_LENGTH))
}

fun TokenDescriptor.toJsonObject() = buildJsonObject {
    put("symbol", symbol.toJsonObject(32))
    put("issuer_name", issuerName.toJsonObject())
}

fun Permission.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(PermissionSurrogate.VALUE_LENGTH))
}

fun BigDecimalAmount<LinearPointer<IvnoTokenType>>.toJsonObject() = buildJsonObject {
    put("quantity", quantity.toJsonObject(20, 4))
    put("token", amountType.toJsonObject())
}

fun Network.toJsonObject(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) = buildJsonObject {
    put("value", value.toJsonObject(NetworkSurrogate.VALUE_LENGTH))
    put("operator", operator.toJsonObject(encodedSize, isAnonymous, scheme))
}

fun Setting<String>.toJsonObject(size: Int) = buildJsonObject {
    put("property", property.toJsonObject(SettingSurrogate.PROPERTY_LENGTH))
    put("value", value.toJsonObject(size))
}

fun Redemption.toJsonObject() = buildJsonObject {
    put("redeemer", redeemer.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("custodian", custodian.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("token_issuing_entity", tokenIssuingEntity.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("amount", amount.toJsonObject())
    put("status", status.toJsonObject())
    put("timestamp", timestamp.toJsonObject())
    put("account_id", accountId.toJsonObject(20))
    put("linear_id", linearId.toJsonObject())
}
