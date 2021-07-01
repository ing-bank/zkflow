package io.ivno.collateraltoken.zinc.types

import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSupportedAlgorithm
import com.ing.zknotary.common.serialization.bfl.serializers.SecureHashSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.UniqueIdentifierSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.resizeTo
import com.ing.zknotary.testing.toJsonArray
import com.ing.zknotary.zinc.types.nullable
import com.ing.zknotary.zinc.types.polymorphic
import com.ing.zknotary.zinc.types.toJsonObject
import io.dasl.contracts.v1.account.AccountAddress
import io.dasl.contracts.v1.token.BigDecimalAmount
import io.dasl.contracts.v1.token.TokenDescriptor
import io.dasl.contracts.v1.token.TokenTransactionSummary.NettedAccountAmount
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.contract.Deposit
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.contract.Redemption
import io.ivno.collateraltoken.contract.Transfer
import io.ivno.collateraltoken.serialization.AccountAddressSurrogate
import io.ivno.collateraltoken.serialization.AttestationPointerSurrogate
import io.ivno.collateraltoken.serialization.AttestationSurrogate
import io.ivno.collateraltoken.serialization.IvnoTokenTypeSurrogate
import io.ivno.collateraltoken.serialization.MembershipSurrogate
import io.ivno.collateraltoken.serialization.NetworkSurrogate
import io.ivno.collateraltoken.serialization.PermissionSurrogate
import io.ivno.collateraltoken.serialization.RoleSurrogate
import io.ivno.collateraltoken.serialization.SettingSurrogate
import io.ivno.collateraltoken.serialization.TokenTransactionSummaryStateSurrogate
import io.kotest.matchers.shouldBe
import io.onixlabs.corda.bnms.contract.Network
import io.onixlabs.corda.bnms.contract.Permission
import io.onixlabs.corda.bnms.contract.Role
import io.onixlabs.corda.bnms.contract.Setting
import io.onixlabs.corda.bnms.contract.membership.Membership
import io.onixlabs.corda.identityframework.contract.AbstractClaim
import io.onixlabs.corda.identityframework.contract.Attestation
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
import net.corda.core.identity.CordaX500Name
import java.security.PublicKey

fun Role.toZincJson() = toJsonObject().toString()
fun TokenDescriptor.toZincJson() = toJsonObject().toString()
fun Permission.toZincJson() = toJsonObject().toString()
@JvmName("BigDecimalAmountWithLinearPointerToZincJson")
fun BigDecimalAmount<LinearPointer<IvnoTokenType>>.toZincJson() = toJsonObject().toString()
@JvmName("BigDecimalAmountWithTokenDescriptorToZincJson")
fun BigDecimalAmount<TokenDescriptor>.toZincJson() = toJsonObject().toString()
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
fun NettedAccountAmount.toZincJson() = toJsonObject().toString()
fun State.toZincJson(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) =
    toJsonObject(encodedSize, isAnonymous, scheme).toString()
fun Attestation<*>.toZincJson() = toJsonObject().toString()

/**
 * Extension function for encoding a nullable ByteArray to Json
 * @param serializedSize The size of the byte array
 *
 * @return the JsonObject of the byte array
 */
fun ByteArray?.toJsonObject(serializedSize: Int) = buildJsonObject {
    put("size", "${this@toJsonObject?.size ?: 0}")
    put("bytes", resizeTo(serializedSize).toJsonArray())
}

/**
 * Extension function for encoding a nullable PublicKey to Json
 * @param encodedSize The size of the inner byte array
 * @param scheme The signature scheme of the key (useful in the case of a null key)
 *
 * @return the JsonObject of the public key
 */
fun PublicKey?.toJsonObject(encodedSize: Int, scheme: SignatureScheme) = buildJsonObject {
    // In the null case there is no way to know the intended implementation class. Given the fact that there are various
    // ways of serializing empty PublicKeys depending on their scheme we need to know somehow which empty version to
    // encode to Json. Thus, the 'scheme' is explicitly passed in this function.
    if (scheme == Crypto.ECDSA_SECP256K1_SHA256 || scheme == Crypto.ECDSA_SECP256R1_SHA256) {
        put("scheme_id", "${this@toJsonObject?.let { scheme.schemeNumberID } ?: 0}")
    }

    put(
        key = "encoded",
        element = this@toJsonObject?.encoded?.toJsonObject(encodedSize)
            ?: ByteArray?::toJsonObject.invoke(null, encodedSize)
    )
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
    if (!isAnonymous) {
        put(
            key = "name",
            element = this@toJsonObject?.let { nameOrNull()?.toJsonObject() }
                ?: CordaX500Name?::toJsonObject.invoke(null)
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
            ?: PublicKey?::toJsonObject.invoke(null, encodedSize, scheme)).polymorphic()
    )
}

fun SecureHash?.toJsonObject(): JsonObject = buildJsonObject {
    put(
        "algorithm",
        this@toJsonObject?.let {"${SecureHashSupportedAlgorithm.fromAlgorithm(algorithm).id}"}
            ?: "${0.toByte()}"
    )
    put("bytes",
        this@toJsonObject?.bytes.toJsonObject(SecureHashSurrogate.BYTES_SIZE)
    )
}

fun StateRef?.toJsonObject() = buildJsonObject {
    put("hash", this@toJsonObject?.txhash.toJsonObject())
    put("index", "${this@toJsonObject?.index ?: 0}")
}

/* This method assumes that the mostSignificantBits of the id are 0 */
fun UniqueIdentifier?.toJsonObject() = buildJsonObject {
    // input validations
    this@toJsonObject?.let { it.id.mostSignificantBits shouldBe 0 }
    put("external_id", this@toJsonObject?.externalId.toJsonObject(UniqueIdentifierSurrogate.EXTERNAL_ID_LENGTH).nullable(this@toJsonObject?.externalId == null))
    put("id", "${this@toJsonObject?.id?.leastSignificantBits ?: 0}")
}

fun Role.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(RoleSurrogate.VALUE_LENGTH))
}

fun TokenDescriptor?.toJsonObject() = buildJsonObject {
    put("symbol", this@toJsonObject?.symbol.toJsonObject(32))
    put("issuer_name", this@toJsonObject?.issuerName.toJsonObject())
}

fun Permission.toJsonObject() = buildJsonObject {
    put("value", value.toJsonObject(PermissionSurrogate.VALUE_LENGTH))
}

@JvmName("BigDecimalAmountWithLinearPointerToJsonObject")
fun BigDecimalAmount<LinearPointer<IvnoTokenType>>.toJsonObject() = buildJsonObject {
    put("quantity", quantity.toJsonObject(20, 4))
    put("token", amountType.toJsonObject())
}

@JvmName("BigDecimalAmountWithTokenDescriptorToJsonObject")
fun BigDecimalAmount<TokenDescriptor>?.toJsonObject() = buildJsonObject {
    put("quantity", this@toJsonObject?.quantity.toJsonObject(20, 4))
    put("token", this@toJsonObject?.amountType.toJsonObject())
}

fun Network.toJsonObject(encodedSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) = buildJsonObject {
    put("value", value.toJsonObject(NetworkSurrogate.VALUE_LENGTH))
    put("operator", operator.toJsonObject(encodedSize, isAnonymous, scheme).polymorphic().nullable(operator == null))
}

fun Setting<String>?.toJsonObject(size: Int) = buildJsonObject {
    put("property", this@toJsonObject?.property.toJsonObject(SettingSurrogate.PROPERTY_LENGTH))
    put("value", this@toJsonObject?.value.toJsonObject(size))
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
    put("reference", reference.toJsonObject(20).nullable(reference == null))
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

fun AccountAddress?.toJsonObject() = buildJsonObject {
    put("account_id", this@toJsonObject?.accountId.toJsonObject(AccountAddressSurrogate.ACCOUNT_ID_LENGTH))
    put("party", this@toJsonObject?.party.toJsonObject())
}

fun AbstractClaim<String>?.toJsonObject(propertyLength: Int, valueLength: Int) = when (this) {
    is Claim<String> -> this.toJsonObject(propertyLength, valueLength).polymorphic()
    else -> TODO("Json encoding is not supported yet for this implementation of AbstractClaim<String>")
}

fun Claim<String>?.toJsonObject(propertyLength: Int, valueLength: Int) = buildJsonObject {
    put("property", this@toJsonObject?.property.toJsonObject(propertyLength))
    put("value", this@toJsonObject?.value.toJsonObject(valueLength))
}

private fun <T: Any> Collection<T>.collectionToJsonObject(
    collectionSize: Int,
    itemToJsonObject: (T?) -> JsonObject,
): JsonObject = buildJsonObject {
    put("size", "$size")
    put(
        "elements",
        buildJsonArray {
            map {
                add(itemToJsonObject.invoke(it))
            }
            repeat(collectionSize - size) {
                add(itemToJsonObject.invoke(null))
            }
        }
    )
}

private fun <K, V> Map<K, V>.mapToJsonObject(collectionSize: Int, entryToJsonObject: (K?, V?) -> JsonObject): JsonObject = buildJsonObject {
    put("size", "$size")
    put(
        "entries",
       buildJsonArray {
           map {
               add(entryToJsonObject.invoke(it.key, it.value))
           }
           repeat(collectionSize - size) {
               add(entryToJsonObject.invoke(null, null))
           }
       }
    )
}

fun Collection<Setting<String>>.toJsonObject(collectionSize: Int, elementSize: Int): JsonObject = collectionToJsonObject(collectionSize) {
    it.toJsonObject(elementSize)
}

fun Collection<AbstractClaim<String>>.toJsonObject(collectionSize: Int, propertyLength: Int, valueLength: Int): JsonObject = collectionToJsonObject(collectionSize) {
    it?.toJsonObject(propertyLength, valueLength)
        ?: (null as Claim<String>?).toJsonObject(propertyLength, valueLength).polymorphic()
}

fun Collection<AbstractParty>.toJsonObject(collectionSize: Int, elementSize: Int, isAnonymous: Boolean, scheme: SignatureScheme) = collectionToJsonObject(collectionSize) {
    it.toJsonObject(
        encodedSize = elementSize,
        isAnonymous = isAnonymous,
        scheme = scheme
    ).polymorphic()
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
    put("previous_state_ref", previousStateRef.toJsonObject().nullable(previousStateRef == null))
}

fun AttestationPointer<*>.toJsonObject(): JsonObject = buildJsonObject {
    put("state_ref", stateRef.toJsonObject())
    put("state_class_name", stateClass.name.toJsonObject(AttestationPointerSurrogate.MAX_CLASS_NAME_SIZE))
    put("state_linear_id", stateLinearId.toJsonObject().nullable(stateLinearId == null))
}

fun NettedAccountAmount?.toJsonObject() = buildJsonObject {
        put("address", this@toJsonObject?.accountAddress.toJsonObject())
        put("amount", this@toJsonObject?.amount.toJsonObject())
}

fun Collection<NettedAccountAmount>.toJsonObject(collectionSize: Int): JsonObject = collectionToJsonObject(collectionSize) {
    it.toJsonObject()
}

fun State.toJsonObject(
    encodedSize: Int,
    isAnonymous: Boolean,
    scheme: SignatureScheme,
) = buildJsonObject {
    put("participants", participants.toJsonObject(TokenTransactionSummaryStateSurrogate.PARTICIPANTS_LENGTH, encodedSize, isAnonymous, scheme))
    put("command", command.toJsonObject(TokenTransactionSummaryStateSurrogate.COMMAND_LENGTH))
    put("amounts", amounts.toJsonObject(TokenTransactionSummaryStateSurrogate.AMOUNTS_LENGTH))
    put("description", description.toJsonObject(TokenTransactionSummaryStateSurrogate.DESCRIPTION_LENGTH))
    put("transaction_time", transactionTime.toJsonObject())
    put("transaction_id", transactionId.toJsonObject().nullable(transactionId == null))
}

fun Attestation<*>.toJsonObject(): JsonObject = buildJsonObject {
    put("attestor", attestor.toJsonObject(EdDSASurrogate.ENCODED_SIZE).polymorphic())
    put("attestees", attestees.toJsonObject(AttestationSurrogate.ATTESTEES_SIZE, EdDSASurrogate.ENCODED_SIZE, true, Crypto.EDDSA_ED25519_SHA512))
    put("pointer", pointer.toJsonObject())
    put("status", status.toJsonObject())
    put("metadata", metadata.toJsonObject(
        collectionSize = AttestationSurrogate.METADATA_MAP_SIZE,
        keyStringSize = AttestationSurrogate.METADATA_KEY_LENGTH,
        valueStringSize = AttestationSurrogate.METADATA_VALUE_LENGTH))
    put("linear_id", linearId.toJsonObject())
    put("previous_state_ref", previousStateRef.toJsonObject().nullable(previousStateRef == null))
}

private fun Map<String, String>.toJsonObject(collectionSize: Int, keyStringSize: Int, valueStringSize: Int): JsonObject = mapToJsonObject(collectionSize) { key, value ->
    buildJsonObject {
        put("key", key.toJsonObject(keyStringSize))
        put("has_value", value != null)
        put("value", value.toJsonObject(valueStringSize))
    }
}
