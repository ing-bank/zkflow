package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.dlt.zkkrypto.util.asUnsigned
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.transactions.ZKProverTransactionFactory.Companion.DEFAULT_PADDING_CONFIGURATION
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.PublicInput
import com.ing.zknotary.common.zkp.Witness
import com.ing.zknotary.common.zkp.ZKNulls
import com.ing.zknotary.common.zkp.fingerprint
import net.corda.client.jackson.internal.readValueAs
import net.corda.core.contracts.Command
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.BLAKE2s256DigestService
import net.corda.core.crypto.PedersenDigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import java.security.PublicKey

class ZincModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(StateAndRef::class.java, StateAndRefMixin::class.java)
        context.setMixInAnnotations(Witness::class.java, WitnessMixin::class.java)
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

        context.setMixInAnnotations(ByteArray::class.java, ByteArrayMixinZinc::class.java)
    }
}

@JsonSerialize(using = StateAndRefMixinSerializer::class)
private interface StateAndRefMixin

private class StateAndRefMixinSerializer : JsonSerializer<StateAndRef<ZKContractState>>() {
    override fun serialize(value: StateAndRef<ZKContractState>, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(
            StateAndRefJson(value.state, value.ref)
        )
    }
}

private class StateAndRefJson(val state: TransactionState<ZKContractState>, val reference: StateRef)

@JsonSerialize(using = PublicInputMixinSerializer::class)
private interface PublicInputMixin

private class PublicInputMixinSerializer : JsonSerializer<PublicInput>() {
    override fun serialize(value: PublicInput, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("transaction_id")
        gen.writeObject(value.transactionId)
        gen.writeFieldName("input_nonces")
        gen.writeObject(value.inputNonces.toList())
        gen.writeFieldName("input_hashes")
        gen.writeObject(value.inputHashes.toList())
        gen.writeFieldName("reference_nonces")
        gen.writeObject(value.referenceNonces.toList())
        gen.writeFieldName("reference_hashes")
        gen.writeObject(value.referenceHashes.toList())
        gen.writeEndObject()
    }
}

@JsonSerialize(using = WitnessMixinSerializer::class)
private interface WitnessMixin

private class WitnessMixinSerializer : JsonSerializer<Witness>() {
    override fun serialize(value: Witness, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("witness")
        gen.writeObject(value.transaction)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = ZKProverTransactionMixinSerializer::class)
private interface ZKProverTransactionMixin

private class ZKProverTransactionMixinSerializer : JsonSerializer<ZKProverTransaction>() {
    override fun serialize(value: ZKProverTransaction, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("transaction")
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
        gen.writeEndObject()
    }
}

/**
 * Only used for testing. Outside testing, ZincSerialization is one way.
 */
private class ZincMixinDeserializer : JsonDeserializer<ZKProverTransaction>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): ZKProverTransaction {
        val json = parser.readValueAs<ZincJson>()
        var paddedInputs = 0
        var paddedOutputs = 0
        var paddedReferences = 0
        var paddedSigners = 0
        return ZKProverTransaction(
            inputs = json.inputs.components.mapNotNull {
                if (it.isFiller) {
                    paddedInputs++
                    null
                } else it.content
            },
            outputs = json.outputs.components.mapNotNull {
                if (it.isFiller) {
                    paddedOutputs++
                    null
                } else it.content
            },
            references = json.references.components.mapNotNull {
                if (it.isFiller) {
                    paddedReferences++
                    null
                } else it.content
            },
            command = Command(
                json.commands.components.single().content,
                json.signers.components.mapNotNull {
                    if (it.isFiller) {
                        paddedSigners++
                        null
                    } else it.content
                }
            ),
            privacySalt = json.privacySalt,

            componentGroupLeafDigestService = BLAKE2s256DigestService,
            nodeDigestService = PedersenDigestService,
            componentPaddingConfiguration = DEFAULT_PADDING_CONFIGURATION,

            // TODO: JSON does not yet include all required parts (notary, networkparametershash)
            notary = ZKNulls.NULL_PARTY,

            // TODO: This does not yet include all components needed for merkle calculation: attachments...
            attachments = emptyList(),
            networkParametersHash = null,
            timeWindow = null
        )
    }
}

// TODO: This does not yet include all components needed for merkle calculation: attachments, networkparams, timewindow
private class ZincJson(
    val inputs: ComponentGroup<PaddingWrapper<StateAndRef<ContractState>>>,
    val outputs: ComponentGroup<PaddingWrapper<TransactionState<ZKContractState>>>,
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
        gen.writeObjectField("bytes", value.fingerprint)
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
