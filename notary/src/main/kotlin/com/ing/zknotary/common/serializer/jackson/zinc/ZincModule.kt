package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import java.security.PublicKey

class ZincModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(ZKProverTransaction::class.java, ZincMixin::class.java)

        context.setMixInAnnotations(PublicKey::class.java, PublicKeyMixinZinc::class.java)
        context.setMixInAnnotations(TransactionState::class.java, TransactionStateMixinZinc::class.java)
        context.setMixInAnnotations(Party::class.java, PartyMixinZinc::class.java)

        context.setMixInAnnotations(SecureHash::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(SecureHash.SHA256::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(PrivacySalt::class.java, PrivacySaltMixinZinc::class.java)

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
                StateGroup(value.padded.inputs, value.merkleTree.groupHashes[ComponentGroupEnum.INPUTS_GROUP.ordinal]),
                StateGroup(value.padded.outputs, value.merkleTree.groupHashes[ComponentGroupEnum.OUTPUTS_GROUP.ordinal]),
                StateGroup(value.padded.references, value.merkleTree.groupHashes[ComponentGroupEnum.REFERENCES_GROUP.ordinal]),
                StateGroup(value.padded.signers, value.merkleTree.groupHashes[ComponentGroupEnum.SIGNERS_GROUP.ordinal]),
                // Currently command serializes into a single Int. This will change in future.
                StateGroup(listOf(0), value.merkleTree.groupHashes[ComponentGroupEnum.COMMANDS_GROUP.ordinal]),
                value.privacySalt
            )
        )
        gen.writeEndObject()
    }
}

private class ZincJson(
    val inputs: StateGroup<ZKStateAndRef<ContractState>>,
    val outputs: StateGroup<ZKStateAndRef<ContractState>>,
    val references: StateGroup<ZKStateAndRef<ContractState>>,
    val signers: StateGroup<PublicKey>,
    val commands: StateGroup<Int>,
    val privacySalt: PrivacySalt
)

class StateGroup<T>(val value: List<T>, val groupHash: SecureHash)

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

private interface CommandMixinZinc {
    val value: CommandData
}

@JsonSerialize(using = ByteArrayMixinZincSerializer::class)
private interface ByteArrayMixinZinc

private class ByteArrayMixinZincSerializer : JsonSerializer<ByteArray>() {
    override fun serialize(value: ByteArray, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(value.map { it.toInt() and 0xFF })
    }
}
