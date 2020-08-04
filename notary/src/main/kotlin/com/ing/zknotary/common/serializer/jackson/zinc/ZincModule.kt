package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.common.contracts.ZKContractState
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.transactions.ZKProverTransaction
import com.ing.zknotary.common.util.PaddingWrapper
import com.ing.zknotary.common.zkp.fingerprint
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.node.services.AttachmentId
import java.security.PublicKey

class ZincModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(ZKProverTransaction::class.java, ZincMixin::class.java)
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

@JsonSerialize(using = ZincMixinSerializer::class)
private interface ZincMixin

private class ZincMixinSerializer : JsonSerializer<ZKProverTransaction>() {
    override fun serialize(value: ZKProverTransaction, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName("witness")
        gen.writeObject(
            ZincJson(
                StateGroup(value.padded.inputs(), value.merkleTree.groupHashes[ComponentGroupEnum.INPUTS_GROUP.ordinal]),
                StateGroup(value.padded.outputs(), value.merkleTree.groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]),
                StateGroup(value.padded.references(), value.merkleTree.groupHashes[ComponentGroupEnum.REFERENCES_GROUP.ordinal]),
                StateGroup(
                    PaddingWrapper.Original(value.command.value),
                    value.merkleTree.groupHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal]
                ),
                StateGroup(value.padded.attachments(), value.merkleTree.groupHashes[ComponentGroupEnum.ATTACHMENTS_GROUP.ordinal]),
                StateSinglet(
                    PaddingWrapper.Original(value.notary),
                    value.merkleTree.groupHashes[ComponentGroupEnum.NOTARY_GROUP.ordinal]
                ),
                StateSinglet(value.padded.timeWindow(), value.merkleTree.groupHashes[ComponentGroupEnum.TIMEWINDOW_GROUP.ordinal]),
                StateSinglet(value.padded.networkParametersHash(), value.merkleTree.groupHashes[ComponentGroupEnum.PARAMETERS_GROUP.ordinal]),
                StateGroup(value.padded.signers(), value.merkleTree.groupHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal]),
                value.privacySalt
            )
        )
        gen.writeEndObject()
    }
}

private class ZincJson(
    val inputs: StateGroup<PaddingWrapper<ZKStateAndRef<ZKContractState>>>,
    val outputs: StateGroup<PaddingWrapper<ZKStateAndRef<ZKContractState>>>,
    val references: StateGroup<PaddingWrapper<ZKStateAndRef<ZKContractState>>>,
    val commands: StateGroup<PaddingWrapper<ZKCommandData>>,
    val attachments: StateGroup<PaddingWrapper<AttachmentId>>,
    val notary: StateSinglet<PaddingWrapper<Party>>,
    val timeWindow: StateSinglet<PaddingWrapper<TimeWindow>>,
    val parameters: StateSinglet<PaddingWrapper<SecureHash>>,
    val signers: StateGroup<PaddingWrapper<PublicKey>>,

    val privacySalt: PrivacySalt
)

data class StateGroup<T>(val value: List<T>, val groupHash: SecureHash) {
    constructor(value: T, groupHash: SecureHash) : this(listOf(value), groupHash)
}

data class StateSinglet<T>(val value: T, val groupHash: SecureHash)

fun ByteArray.asBytes255(): IntArray = this.map { it.toInt() and 0xFF }.toIntArray()
fun IntArray.asString() = this.joinToString(", ", "[", "]")

@JsonSerialize(using = PublicKeyMixinZincSerializer::class)
private interface PublicKeyMixinZinc

private class PublicKeyMixinZincSerializer : JsonSerializer<PublicKey>() {
    override fun serialize(value: PublicKey, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.encoded)
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
        gen.writeObjectField("owningKey", value.owningKey)
        gen.writeEndObject()
    }
}

@JsonSerialize(using = SecureHashMixinZincSerializer::class)
private interface SecureHashMixinZinc

private class SecureHashMixinZincSerializer : JsonSerializer<SecureHash>() {
    override fun serialize(value: SecureHash, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.bytes)
    }
}

@JsonSerialize(using = PrivacySaltMixinZincSerializer::class)
private interface PrivacySaltMixinZinc

private class PrivacySaltMixinZincSerializer : JsonSerializer<PrivacySalt>() {
    override fun serialize(value: PrivacySalt, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.bytes)
    }
}

@JsonSerialize(using = TimeWindowMixinZincSerializer::class)
private interface TimeWindowMixinZinc

private class TimeWindowMixinZincSerializer : JsonSerializer<TimeWindow>() {
    override fun serialize(value: TimeWindow, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.fingerprint)
    }
}

@JsonSerialize(using = ByteArrayMixinZincSerializer::class)
private interface ByteArrayMixinZinc

private class ByteArrayMixinZincSerializer : JsonSerializer<ByteArray>() {
    override fun serialize(value: ByteArray, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.map { it.toInt() and 0xFF })
    }
}
