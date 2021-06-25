package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.CordaX500NameSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSupportedAlgorithm
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.resizeTo
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.zinc.types.polymorphic
import com.ing.zknotary.zinc.types.toJsonObject
import io.dasl.contracts.v1.account.AccountAddress
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.serialization.AccountAddressSurrogate
import io.ivno.collateraltoken.serialization.AttestationPointerSurrogate
import io.ivno.collateraltoken.serialization.IvnoTokenTypeSurrogate
import io.ivno.collateraltoken.serialization.MembershipSurrogate
import io.ivno.collateraltoken.serialization.NetworkSurrogate
import io.ivno.collateraltoken.serialization.PermissionSurrogate
import io.ivno.collateraltoken.serialization.RoleSurrogate
import io.ivno.collateraltoken.serialization.SettingSurrogate
import io.kotest.matchers.shouldBe
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Permission
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.AttestationPointer
import io.onixlabs.corda.identityframework.contract.Claim
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.contracts.LinearPointer
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SecureHash
import net.corda.core.crypto.SignatureScheme
import net.corda.core.crypto.algorithm
import net.corda.core.identity.AbstractParty
import java.security.PublicKey

fun Role.toZincJson() = toJsonObject().toString()
fun TokenDescriptor.toZincJson() = toJsonObject().toString()
fun Permission.toZincJson() = toJsonObject().toString()
fun BigDecimalAmount<LinearPointer<IvnoTokenType>>.toZincJson() = toJsonObject().toString()
fun Network.toZincJson(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) =
    toJsonObject(encodedSize, isAnonymous, scheme).toString()
fun Setting<String>.toZincJson(size: Int) = toJsonObject(size).toString()
fun Redemption.toZincJson() = toJsonObject().toString()
fun IvnoTokenType.toZincJson(
    networkEncodedSize: Int,
    isAnonymous: Boolean,
    networkScheme: SignatureScheme,
    custodianEncodedSize: Int,
    custodianScheme: SignatureScheme,
    tokenIssuingEntityEncodedSize: Int,
    tokenIssuingEntityScheme: SignatureScheme
) = toJsonObject(
    networkEncodedSize,
    isAnonymous,
    networkScheme,
    custodianEncodedSize,
    custodianScheme,
    tokenIssuingEntityEncodedSize,
    tokenIssuingEntityScheme,
).toString()
fun Deposit.toZincJson() = toJsonObject().toString()
fun Transfer.toZincJson() = toJsonObject().toString()
fun AccountAddress.toZincJson() = toJsonObject().toString()
fun AbstractClaim<String>.toZincJson(propertyLength: Int, valueLength: Int) =
    toJsonObject(propertyLength, valueLength).toString()
fun Membership.toZincJson(
    networkEncodedSize: Int,
    isNetworkAnonymous: Boolean,
    networkScheme: SignatureScheme,
    holderEncodedSize: Int,
    isHolderAnonymous: Boolean,
    holderScheme: SignatureScheme,
    identityPropertyLength: Int,
    identityValueLength: Int,
    settingsValueLength: Int,
) = toJsonObject(
    networkEncodedSize,
    isNetworkAnonymous,
    networkScheme,
    holderEncodedSize,
    isHolderAnonymous,
    holderScheme,
    identityPropertyLength,
    identityValueLength,
    settingsValueLength,
).toString()
fun AttestationPointer<*>.toZincJson() = toJsonObject().toString()

/**
 * Extension function for encoding a nullable ByteArray to Json
 * @param serializedSize The size of the byte array
 * @param isNullable Boolean flag indicating whether the nullability wrapper will be applied on the Json structure
 *
 * @return the JsonObject of the byte array
 */
fun ByteArray?.toJsonObject(serializedSize: Int, isNullable: Boolean = false) = buildJsonObject {
    val inner = buildJsonObject {
        put("size", "${this@toJsonObject?.size ?: 0}")
        put("bytes", resizeTo(serializedSize).toJsonArray())
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

/**
 * Extension function for encoding a nullable PublicKey to Json
 * @param encodedSize The size of the inner byte array
 * @param scheme The signature scheme of the key (useful in the case of a null key)
 * @param isNullable Boolean flag indicating whether the nullability wrapper will be applied on the Json structure
 *
 * @return the JsonObject of the public key
 */
fun PublicKey?.toJsonObject(encodedSize: Int, scheme: SignatureScheme, isNullable: Boolean = false) = buildJsonObject {
    // In the null case there is no way to know the intended implementation class. Given the fact that there are various
    // ways of serializing empty PublicKeys depending on their scheme we need to know somehow which empty version to
    // encode to Json. Thus, the 'scheme' is explicitly passed in this function.
    val inner = buildJsonObject {
        if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
            put("scheme_id", "${this@toJsonObject?.let { scheme.schemeNumberID } ?: 0}")
        }

        put(
            key = "encoded",
            element = this@toJsonObject?.encoded?.toJsonObject(encodedSize)
                ?: ByteArray?::toJsonObject.invoke(null, encodedSize, false)
        )
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner.polymorphic())
}

/**
 * Extension function for encoding a nullable AbstractParty to Json
 * @param encodedSize The size of the inner byte array of the owning key
 * @param isAnonymous Boolean flag indicating the implementation class of the abstract party (useful in the case of a
 * null abstract party)
 * @param scheme The signature scheme of the owning key (useful in the case of a null abstract party)
 * @param isNullable Boolean flag indicating whether the nullability wrapper will be applied on the Json structure
 *
 * @return the JsonObject of the abstract party
 */
fun AbstractParty?.toJsonObject(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme, isNullable: Boolean = false) = buildJsonObject {
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
                ?: PublicKey?::toJsonObject.invoke(null, encodedSize, scheme, false)).polymorphic()
        )
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner.polymorphic())
}

fun SecureHash?.toJsonObject(isNullable: Boolean = false): JsonObject = buildJsonObject {
    val inner = buildJsonObject {
        put(
            "algorithm",
            this@toJsonObject?.let {"${SecureHashSupportedAlgorithm.fromAlgorithm(algorithm).id}"}
                ?: "${0.toByte()}"
        )
        put("bytes",
            this@toJsonObject?.bytes.toJsonObject(SecureHashSurrogate.BYTES_SIZE)
        )
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

fun StateRef?.toJsonObject(isNullable: Boolean = false) = buildJsonObject {
    val inner = buildJsonObject {
        put("hash", this@toJsonObject?.txhash.toJsonObject())
        put("index", "${this@toJsonObject?.index ?: 0}")
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

/* This method assumes that the mostSignificantBits of the id are 0 */
fun UniqueIdentifier?.toJsonObject(isNullable: Boolean = false) = buildJsonObject {
    val inner = buildJsonObject {
        // input validations
        this@toJsonObject?.let { it.id.mostSignificantBits shouldBe 0 }
        put("external_id", this@toJsonObject?.externalId.toJsonObject(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH, true))
        put("id", "${this@toJsonObject?.id?.leastSignificantBits ?: 0}")
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
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
    put("operator", operator.toJsonObject(encodedSize, isAnonymous, scheme, true))
}

fun Setting<String>?.toJsonObject(size: Int, isNullable: Boolean = false) = buildJsonObject {
    val inner = buildJsonObject {
        put("property", this@toJsonObject?.property.toJsonObject(SettingSurrogate.PROPERTY_LENGTH))
        put("value", this@toJsonObject?.value.toJsonObject(size))
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
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

fun IvnoTokenType.toJsonObject(
    networkEncodedSize: Int,
    isAnonymous: Boolean,
    networkScheme: SignatureScheme,
    custodianEncodedSize: Int,
    custodianScheme: SignatureScheme,
    tokenIssuingEntityEncodedSize: Int,
    tokenIssuingEntityScheme: SignatureScheme,
) = buildJsonObject {
    put("network", network.toJsonObject(networkEncodedSize, isAnonymous, networkScheme))
    put("custodian", custodian.toJsonObject(custodianEncodedSize, false, custodianScheme))
    put("token_issuing_entity", tokenIssuingEntity.toJsonObject(tokenIssuingEntityEncodedSize, false, tokenIssuingEntityScheme))
    put("display_name", displayName.toJsonObject(IvnoTokenTypeSurrogate.DISPLAY_NAME_LENGTH))
    put("fraction_digits", "$fractionDigits")
    put("linear_id", linearId.toJsonObject())
}

fun Deposit.toJsonObject() = buildJsonObject {
    put("depositor", depositor.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("custodian", custodian.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("token_issuing_entity", tokenIssuingEntity.toJsonObject(EdDSASurrogate.ENCODED_SIZE))
    put("amount", amount.toJsonObject())
    put("reference", reference.toJsonObject(20, true))
    put("status", status.toJsonObject())
    put("timestamp", timestamp.toJsonObject())
    put("account_id", accountId.toJsonObject(20))
    put("linear_id", linearId.toJsonObject())
}

fun Transfer.toJsonObject() = buildJsonObject {
    put("current_token_holder", currentTokenHolder.toJsonObject(EdDSASurrogate.ENCODED_SIZE).polymorphic())
    put("target_token_holder", targetTokenHolder.toJsonObject(EdDSASurrogate.ENCODED_SIZE).polymorphic())
    put("initiator", initiator.toJsonObject())
    put("amount", amount.toJsonObject())
    put("status", status.toJsonObject())
    put("timestamp", timestamp.toJsonObject())
    put("current_token_holder_account_id", currentTokenHolderAccountId.toJsonObject(20))
    put("target_token_holder_account_id", targetTokenHolderAccountId.toJsonObject(20))
    put("linear_id", linearId.toJsonObject())
}

fun AccountAddress.toJsonObject() = buildJsonObject {
    put("account_id", accountId.toJsonObject(AccountAddressSurrogate.ACCOUNT_ID_LENGTH))
    put("party", party.toJsonObject())
}

fun AbstractClaim<String>.toJsonObject(propertyLength: Int, valueLength: Int) = when (this) {
    is Claim<String> -> this.toJsonObject(propertyLength, valueLength, false).polymorphic()
    else -> TODO("Json encoding is not supported yet for ${this::class}")
}

fun Claim<String>?.toJsonObject(propertyLength: Int, valueLength: Int, isNullable: Boolean = false) = buildJsonObject {
    val inner = buildJsonObject {
        put("property", this@toJsonObject?.property.toJsonObject(propertyLength))
        put("value", this@toJsonObject?.value.toJsonObject(valueLength))
    }

    if (!isNullable) return@toJsonObject inner
    put("is_null", this@toJsonObject == null)
    put("inner", inner)
}

fun Collection<Setting<String>>.toJsonObject(collectionSize: Int, elementSize: Int): JsonObject = buildJsonObject {
    put("size", "$size")
    put(
        "elements",
        buildJsonArray {
            map {
                add(it.toJsonObject(elementSize))
            }
            repeat(collectionSize - size) {
                add((null as Setting<String>?).toJsonObject(elementSize))
            }
        }
    )
}

fun Collection<AbstractClaim<String>>.toJsonObject(collectionSize: Int, propertyLength: Int, valueLength: Int): JsonObject = buildJsonObject {
    put("size", "$size")
    put(
        "elements",
        buildJsonArray {
            map {
                add(it.toJsonObject(propertyLength, valueLength))
            }
            repeat(collectionSize - size) {
                add((null as Claim<String>?).toJsonObject(propertyLength, valueLength).polymorphic())
            }
        }
    )
}

fun Membership.toJsonObject(
    networkEncodedSize: Int,
    isNetworkAnonymous: Boolean,
    networkScheme: SignatureScheme,
    holderEncodedSize: Int,
    isHolderAnonymous: Boolean,
    holderScheme: SignatureScheme,
    identityPropertyLength: Int,
    identityValueLength: Int,
    settingsValueLength: Int,
) = buildJsonObject {
    put("network", network.toJsonObject(networkEncodedSize, isNetworkAnonymous, networkScheme))
    put("holder", holder.toJsonObject(holderEncodedSize, isHolderAnonymous, holderScheme).polymorphic())
    put("identity", (identity as Set<AbstractClaim<String>>).toJsonObject(MembershipSurrogate.IDENTITY_LENGTH ,identityPropertyLength, identityValueLength))
    put("settings", (settings as Set<Setting<String>>).toJsonObject(MembershipSurrogate.SETTINGS_LENGTH, settingsValueLength))
    put("linear_id", linearId.toJsonObject())
    put("previous_state_ref", previousStateRef.toJsonObject(true))
}

fun AttestationPointer<*>.toJsonObject(): JsonObject = buildJsonObject {
    put("state_ref", stateRef.toJsonObject())
    put("state_class_name", stateClass.name.toJsonObject(AttestationPointerSurrogate.MAX_CLASS_NAME_SIZE))
    put("state_linear_id", stateLinearId.toJsonObject(true))
}