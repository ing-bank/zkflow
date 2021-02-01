package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.dactyloscopy.fingerprint
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import org.bouncycastle.jce.provider.BouncyCastleProvider
import sun.misc.DoubleConsts
import java.math.BigDecimal
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Security

class ZincModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(StateAndRef::class.java, StateAndRefMixin::class.java)
        context.setMixInAnnotations(Witness::class.java, WitnessMixin::class.java)
        context.setMixInAnnotations(Amount::class.java, AmountMixin::class.java)
        context.setMixInAnnotations(PublicInput::class.java, PublicInputMixin::class.java)
        context.setMixInAnnotations(ZKProverTransaction::class.java, ZKProverTransactionMixin::class.java)
        context.setMixInAnnotations(ZKCommandData::class.java, ZKCommandDataMixinZinc::class.java)

        context.setMixInAnnotations(PublicKey::class.java, PublicKeyMixinZinc::class.java)
        context.setMixInAnnotations(TransactionState::class.java, TransactionStateMixinZinc::class.java)
        context.setMixInAnnotations(Party::class.java, PartyMixinZinc::class.java)

        context.setMixInAnnotations(SecureHash::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(SecureHash.SHA256::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(PrivacySalt::class.java, PrivacySaltMixinZinc::class.java)
        context.setMixInAnnotations(TimeWindow::class.java, TimeWindowMixinZinc::class.java)

        context.setMixInAnnotations(BigDecimal::class.java, BigDecimalMixin::class.java)
        context.setMixInAnnotations(ByteArray::class.java, ByteArrayMixinZinc::class.java)
        context.setMixInAnnotations(Double::class.java, DoubleMixinZinc::class.java)
    }
}

@JsonSerialize(using = StateAndRefMixinSerializer::class)
private interface StateAndRefMixin

private class StateAndRefMixinSerializer : JsonSerializer<StateAndRef<ContractState>>() {
    override fun serialize(value: StateAndRef<ContractState>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(
            StateAndRefJson(value.state, value.ref)
        )
    }
}

private class StateAndRefJson(val state: TransactionState<ContractState>, val reference: StateRef)

@JsonSerialize(using = PublicInputMixinSerializer::class)
private interface PublicInputMixin

private class PublicInputMixinSerializer : JsonSerializer<PublicInput>() {
    override fun serialize(value: PublicInput, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("transaction_id")
        gen.writeObject(value.transactionId)
        gen.writeFieldName("input_hashes")
        gen.writeObject(value.inputHashes)
        gen.writeFieldName("reference_hashes")
        gen.writeObject(value.referenceHashes)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = WitnessMixinSerializer::class)
private interface WitnessMixin

private class WitnessMixinSerializer : JsonSerializer<Witness>() {
    override fun serialize(value: Witness, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("witness")
        gen.writeObject(WitnessJson(value.transaction, value.inputNonces, value.referenceNonces))
        gen.writeEndObject()
    }
}

@Suppress("unused")
private class WitnessJson(
    val transaction: ZKProverTransaction,
    val inputNonces: List<SecureHash>,
    val referenceNonces: List<SecureHash>
)

@JsonSerialize(using = AmountMixinSerializer::class)
private interface AmountMixin

private class AmountMixinSerializer : JsonSerializer<Amount<Any>>() {
    override fun serialize(value: Amount<Any>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("quantity", value.quantity)
        gen.writeObjectField("display_token_size", value.displayTokenSize)
        gen.writeObjectField("token_name_hash", hashTokenName(value.token::class.java.toString()))
        gen.writeEndObject()
    }

    private fun hashTokenName(tokenName: String): ByteArray {
        Security.addProvider(BouncyCastleProvider())
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(tokenName.toByteArray())
        val digest = messageDigest.digest()
        return digest
    }
}

@JsonSerialize(using = ZKProverTransactionMixinSerializer::class)
private interface ZKProverTransactionMixin

private class ZKProverTransactionMixinSerializer : JsonSerializer<ZKProverTransaction>() {
    override fun serialize(value: ZKProverTransaction, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(
            ZincJson(
                ComponentGroup(
                    value.padded.inputs(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.INPUTS_GROUP.ordinal]
                ),
                ComponentGroup(
                    value.padded.outputs(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]
                ),
                ComponentGroup(
                    value.padded.references(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.REFERENCES_GROUP.ordinal]
                ),
                ComponentGroup(
                    PaddingWrapper.Original(value.command.value),
                    value.merkleTree.groupHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal]
                ),
                ComponentGroup(
                    value.padded.attachments(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal]
                ),
                ComponentSinglet(
                    PaddingWrapper.Original(value.notary),
                    value.merkleTree.groupHashes[ComponentGroupEnum.NOTARY_GROUP.ordinal]
                ),
                ComponentSinglet(
                    value.padded.timeWindow(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]
                ),
                ComponentSinglet(
                    value.padded.networkParametersHash(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]
                ),
                ComponentGroup(
                    value.padded.signers(),
                    value.merkleTree.groupHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal]
                ),
                value.privacySalt
            )
        )
    }
}

private data class ZincJson(
    val inputs: ComponentGroup<PaddingWrapper<StateAndRef<ContractState>>>,
    val outputs: ComponentGroup<PaddingWrapper<TransactionState<ContractState>>>,
    val references: ComponentGroup<PaddingWrapper<StateAndRef<ContractState>>>,
    val commands: ComponentGroup<PaddingWrapper<ZKCommandData>>,
    val attachments: ComponentGroup<PaddingWrapper<AttachmentId>>,
    val notary: ComponentSinglet<PaddingWrapper<Party>>,
    val timeWindow: ComponentSinglet<PaddingWrapper<TimeWindow>>,
    val parameters: ComponentSinglet<PaddingWrapper<SecureHash>>,
    val signers: ComponentGroup<PaddingWrapper<PublicKey>>,
    val privacySalt: PrivacySalt
)

data class ComponentGroup<T>(val components: List<T>, val groupHash: SecureHash) {
    constructor(components: T, groupHash: SecureHash) : this(listOf(components), groupHash)
}

data class ComponentSinglet<T>(val component: T, val groupHash: SecureHash)

@JsonSerialize(using = PublicKeyMixinZincSerializer::class)
private interface PublicKeyMixinZinc

private class PublicKeyMixinZincSerializer : JsonSerializer<PublicKey>() {
    override fun serialize(value: PublicKey, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("bytes", value.encoded)
        gen.writeEndObject()
    }
}

@JsonIgnoreProperties(value = ["contract", "encumbrance", "constraint"])
private interface TransactionStateMixinZinc

@JsonSerialize(using = ZKCommandDataZincSerializer::class)
private interface ZKCommandDataMixinZinc

private class ZKCommandDataZincSerializer : JsonSerializer<ZKCommandData>() {
    override fun serialize(value: ZKCommandData, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(value.id)
    }
}

@JsonSerialize(using = PartyMixinZincSerializer::class)
private interface PartyMixinZinc

private class PartyMixinZincSerializer : JsonSerializer<Party>() {
    override fun serialize(value: Party, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("owning_key", value.owningKey)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = SecureHashMixinZincSerializer::class)
private interface SecureHashMixinZinc

private class SecureHashMixinZincSerializer : JsonSerializer<SecureHash>() {
    override fun serialize(value: SecureHash, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("bytes", value.bytes)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = PrivacySaltMixinZincSerializer::class)
private interface PrivacySaltMixinZinc

private class PrivacySaltMixinZincSerializer : JsonSerializer<PrivacySalt>() {
    override fun serialize(value: PrivacySalt, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("bytes", value.bytes)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = TimeWindowMixinZincSerializer::class)
private interface TimeWindowMixinZinc

private class TimeWindowMixinZincSerializer : JsonSerializer<TimeWindow>() {
    override fun serialize(value: TimeWindow, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("bytes", value.fingerprint())
        gen.writeEndObject()
    }
}

private const val BIG_DECIMAL_INTEGER_SIZE = 1024
private const val BIG_DECIMAL_FRACTION_SIZE = 128

@JsonSerialize(using = BigDecimalMixinSerializer::class)
private interface BigDecimalMixin

private class BigDecimalMixinSerializer : JsonSerializer<BigDecimal>() {
    override fun serialize(value: BigDecimal, gen: JsonGenerator, serializers: SerializerProvider) {
        val stringRepresentation = value.toPlainString()
        val integerFractionTuple = stringRepresentation.removePrefix("-").split(".")

        val integer = IntArray(BIG_DECIMAL_INTEGER_SIZE)
        val startingIdx = BIG_DECIMAL_INTEGER_SIZE - integerFractionTuple[0].length
        integerFractionTuple[0].forEachIndexed { idx, char ->
            integer[startingIdx + idx] = Character.getNumericValue(char)
        }

        val fraction = IntArray(BIG_DECIMAL_FRACTION_SIZE)
        if (integerFractionTuple.size == 2) {
            integerFractionTuple[1].forEachIndexed { idx, char ->
                fraction[idx] = Character.getNumericValue(char)
            }
        }

        gen.writeStartObject()
        gen.writeObjectField("sign", value.signum())
        gen.writeObjectField("integer", integer)
        gen.writeObjectField("fraction", fraction)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = ByteArrayMixinZincSerializer::class)
private interface ByteArrayMixinZinc

private class ByteArrayMixinZincSerializer : JsonSerializer<ByteArray>() {
    override fun serialize(value: ByteArray, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.map { it.asUnsigned() })
    }
}

@JsonSerialize(using = DoubleMixinZincSerializer::class)
private interface DoubleMixinZinc

private class DoubleMixinZincSerializer : JsonSerializer<Double>() {
    override fun serialize(value: Double, gen: JsonGenerator, serializers: SerializerProvider) {
        val doubleBits = java.lang.Double.doubleToLongBits(value)
        val magnitude = doubleBits and DoubleConsts.SIGNIF_BIT_MASK
        val exponent = doubleBits and DoubleConsts.EXP_BIT_MASK shr 52
        val sign =
            if (magnitude == 0L && exponent == 0L) 0
            else if (doubleBits and DoubleConsts.SIGNIF_BIT_MASK shr 63 == 0L) 1
            else -1

        gen.writeStartObject()
        gen.writeObjectField("sign", sign)
        gen.writeObjectField("exponent", exponent)
        gen.writeObjectField("magnitude", magnitude)
        gen.writeEndObject()
    }
}
