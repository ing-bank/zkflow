package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.zknotary.common.contracts.TestContract
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.core.contracts.Command
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.security.PublicKey

class ZincModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(ZKProverTransaction::class.java, ZincMixin::class.java)

        context.setMixInAnnotations(PublicKey::class.java, PublicKeyMixinZinc::class.java)
        context.setMixInAnnotations(TransactionState::class.java, TransactionStateMixinZinc::class.java)
        context.setMixInAnnotations(Party::class.java, PartyMixinZinc::class.java)

        context.setMixInAnnotations(Command::class.java, CommandMixinZinc::class.java)
        context.setMixInAnnotations(CommandData::class.java, CommandDataMixinZinc::class.java)

        context.setMixInAnnotations(SecureHash::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(SecureHash.SHA256::class.java, SecureHashMixinZinc::class.java)
        context.setMixInAnnotations(PrivacySalt::class.java, PrivacySaltMixinZinc::class.java)

        context.setMixInAnnotations(ByteArray::class.java, ByteArrayMixinZinc::class.java)

        context.setMixInAnnotations(Status::class.java, StatusMixin::class.java)
    }
}

@JsonSerialize(using = ZincMixinSerializer::class)
private interface ZincMixin

private class ZincMixinSerializer : JsonSerializer<ZKProverTransaction>() {
    override fun serialize(value: ZKProverTransaction, gen: JsonGenerator, serializers: SerializerProvider) {
        // Prepare to read required dimensions.
        val cwd = System.getProperty("user.dir")
        val dims = Paths.get("$cwd/../prover/ZKMerkleTree/src/ComponentGroups.zn")
        val content = String(Files.readAllBytes(dims), Charset.defaultCharset())

        val inputsSize = extractDim("N_INPUTS", content)
        assert(inputsSize != null)
        val inputsGroup = Group(
            ComponentGroupEnum.INPUTS_GROUP,
            value.inputs.constrain(inputsSize!!, ZKPrimitive.ZKStateAndRef),
            value.componentGroupLeafDigestService, value.privacySalt
        )

        val outputsSize = extractDim("N_OUTPUTS", content)
        assert(outputsSize != null)
        val outputsGroup = Group(
            ComponentGroupEnum.OUTPUTS_GROUP,
            value.outputs.constrain(outputsSize!!, ZKPrimitive.ZKStateAndRef),
            value.componentGroupLeafDigestService, value.privacySalt
        )

        val referencesSize = extractDim("N_REFERENCES", content)
        assert(referencesSize != null)
        val referencesGroup = Group(
            ComponentGroupEnum.REFERENCES_GROUP,
            value.references.constrain(referencesSize!!, ZKPrimitive.ZKStateAndRef),
            value.componentGroupLeafDigestService, value.privacySalt
        )

        // Only one command is expected.
        val command = value.command
        val participantsSize = extractDim("N_SIGNERS", content)
        assert(participantsSize != null)
        val commandsGroup = CommandGroup(command, participantsSize!!, value.componentGroupLeafDigestService, value.privacySalt)

        val leaves = listOf(
            inputsGroup.groupHash, outputsGroup.groupHash,
            commandsGroup.dataGroupHash, referencesGroup.groupHash,
            commandsGroup.signerGroupHash, SecureHash.zeroHash.bytes
        )
        val root = leaves.merkelize(value.nodeDigestService)

//        println("inputs = ${leaves[0].asBytes255().asString()}\n")
//        println("outputs = ${leaves[1].asBytes255().asString()}\n")
//        println("commands data = ${leaves[2].asBytes255().asString()}\n")
//        println("references = ${leaves[3].asBytes255().asString()}\n")
//        println("signers = ${leaves[4].asBytes255().asString()}\n")

        println("root = ${root.asBytes255().asString()}\n")

        gen.writeStartObject()
        gen.writeFieldName("witness")
        gen.writeObject(
            ZincJson(
                inputsGroup,
                outputsGroup,
                referencesGroup,
                commandsGroup,
                value.privacySalt
            )
        )
        gen.writeEndObject()
    }

    fun extractDim(name: String, content: String): Int? =
        """$name.+(\d+)"""
            .toRegex()
            .find(content)
            ?.groups
            ?.get(1)
            ?.value
            ?.toIntOrNull()
}

private class ZincJson(
    val inputs: Group<Status<ZKStateAndRef<ContractState>>>,
    val outputs: Group<Status<ZKStateAndRef<ContractState>>>,
    val references: Group<Status<ZKStateAndRef<ContractState>>>,
    val commands: CommandGroup,
    val privacySalt: PrivacySalt
)

class Group<T> (componentGroup: ComponentGroupEnum, val value: List<T>, digestService: DigestService, privacySalt: PrivacySalt) {
    val groupHash: ByteArray

    init {
        assert(value.isNotEmpty())

        val leaves = if (
            componentGroup == ComponentGroupEnum.INPUTS_GROUP ||
            componentGroup == ComponentGroupEnum.REFERENCES_GROUP
        ) {
            val empty = SecureHash.zeroHash.bytes
            val l = value.map {
                when (it) {
                    is Status.Defined<*> -> (it.value as ZKStateAndRef<ContractState>).ref.id.bytes
                    is Status.Undefined<*> -> empty
                    else -> throw Error("Unreachable")
                }
            }
            l.constrain(empty, l.size + l.size % 2)
        } else if (componentGroup == ComponentGroupEnum.OUTPUTS_GROUP) {
            // two public keys (44 bytes) and one u32 (4 bytes) worth of bytes
            val empty = ByteBuffer.allocate(44 + 44 + 4).array()
            val l = value.map {
                when (it) {
                    is Status.Defined<*> -> {
                        val zkStateAndRef = it.value as ZKStateAndRef<TestContract.TestState>
                        zkStateAndRef.state.data.owner.owningKey.encoded +
                            zkStateAndRef.state.notary.owningKey.encoded +
                            ByteBuffer.allocate(4).putInt(zkStateAndRef.state.data.value).array()
                    }
                    is Status.Undefined<*> -> empty
                    else -> throw Error("Unreachable")
                }
            }
            l
        } else {
            throw Error("Unknown ComponentGroup")
        }

//        println("Privacy Salt")
//        println(privacySalt.bytes.joinToString(", "))
//        println()
//        println(privacySalt.bytes.asBytes255().joinToString(", "))

        groupHash = leaves.mapIndexed { i, leaf ->
            val unique = ByteBuffer.allocate(8).putInt(componentGroup.ordinal).putInt(i).array()
            val nonce = digestService.hash(privacySalt.bytes + unique)
            val digest = digestService.hash(nonce.bytes + leaf)

//            if (componentGroup == ComponentGroupEnum.OUTPUTS_GROUP){
//                println("LEAF $i: ${leaf.asBytes255().joinToString(", ")}")
//                println("UNIQUE $i: ${unique.asBytes255().joinToString(", ")}")
//                println("NONCE $i: ${nonce.bytes.asBytes255().joinToString(", ")}")
//                println("DIGEST $i: ${digest.bytes.asBytes255().joinToString(", ")}")
//                println()
//            }

            digest.bytes
        }.merkelize(digestService)
    }
}

class CommandGroup(command: Command<*>, participantsSize: Int, digestService: DigestService, privacySalt: PrivacySalt) {
    data class Cmd(val data: Int, val signers: List<Status<PublicKey>>)
    val value: Cmd

    val dataGroupHash: ByteArray
    val signerGroupHash: ByteArray

    init {
        // TODO ideally this enum must be inside the CommandData
        val cmdData = when (command.value) {
            is TestContract.Create -> 0
            is TestContract.Move -> 1
            else -> throw Error("Unsupported command")
        }

        value = Cmd(cmdData, command.signers.constrain(participantsSize, ZKPrimitive.PublicKey))

        dataGroupHash = run {
            val unique = ByteBuffer.allocate(8).putInt(ComponentGroupEnum.COMMANDS_GROUP.ordinal).putInt(0).array()
            val nonce = digestService.hash(privacySalt.bytes + unique)
            val digest = digestService.hash(nonce.bytes + ByteBuffer.allocate(4).putInt(value.data).array()).bytes
            listOf(digest, ByteBuffer.allocate(32).array())
        }.merkelize(digestService)

        val leaves = value.signers.map { status ->
            when (status) {
                is Status.Undefined -> ByteBuffer.allocate(44).array()
                is Status.Defined -> status.value.encoded
            }
        }.mapIndexed { i, leaf ->
            val unique = ByteBuffer.allocate(8).putInt(ComponentGroupEnum.SIGNERS_GROUP.ordinal).putInt(i).array()
            val nonce = digestService.hash(privacySalt.bytes + unique)
            val digest = digestService.hash(nonce.bytes + leaf)

            digest.bytes
        }

        signerGroupHash = leaves
            .merkelize(digestService)
    }
}

fun ByteArray.asBytes255(): IntArray = this.map { it.toInt() and 0xFF }.toIntArray()
fun IntArray.asString() = this.joinToString(", ", "[", "]")

fun <T> List<T>.constrain(size: Int, primitive: ZKPrimitive) = List(size) {
    if (it < this.size)
        Status.Defined(primitive, this[it])
    else
    // This type annotation is needed here!
    // Otherwise compiler produce the following error
    // Type mismatch: inferred type is
    // Group<Status.Defined<ZKStateAndRef<ContractState>>> but
    // Group<Status<ZKStateAndRef<ContractState>>> was expected.
    // Although Status.Defined is Status.
        Status.Undefined<T>(primitive)
}

fun <T> List<T>.constrain(default: T, size: Int) = List(size) {
    if (it < this.size) this[it] else default
}

fun List<ByteArray>.merkelize(digestService: DigestService): ByteArray {
    assert(this.size > 1)
    this.forEach { assert(it.size == 32) }

    var level = this

    do {
        level = level.constrain(SecureHash.zeroHash.bytes, level.size + level.size % 2)

//        level.forEachIndexed { i, bytes ->
//            println("$i = ${bytes.asBytes255().asString()}\n")
//        }

        level = level
            .windowed(2, 2)
            .map { digestService.hash(it[0] + it[1]).bytes }
    } while (level.size != 1)

    return level.first()
}

sealed class Status<T>(val primitive: ZKPrimitive) {
    class Defined<T>(primitive: ZKPrimitive, val value: T) : Status<T>(primitive)
    class Undefined<T>(primitive: ZKPrimitive) : Status<T>(primitive)

    fun serialize(gen: JsonGenerator) {
        if (primitive == ZKPrimitive.ZKStateAndRef) {
            when (this) {
                is Defined -> gen.writeObject(this.value as ZKStateAndRef<ContractState>)
                is Undefined -> ZKPrimitive.ZKStateAndRef.default(gen)
            }
        } else if (primitive == ZKPrimitive.PublicKey) {
            when (this) {
                is Defined -> gen.writeObject(this.value as PublicKey)
                is Undefined -> ZKPrimitive.PublicKey.default(gen)
            }
        }
    }
}

enum class ZKPrimitive {
    ZKStateAndRef {
        override fun default(gen: JsonGenerator) {
            val emptyPk = IntArray(44) { 0 }
            val emptyHash = SecureHash.zeroHash
            gen.writeStartObject()
            // state
            gen.writeFieldName("state")
            gen.writeStartObject()
            gen.writeFieldName("data")
            gen.writeStartObject()
            gen.writeFieldName("owner")
            gen.writeStartObject()
            gen.writeObjectField("owningKey", emptyPk)
            gen.writeEndObject()

            gen.writeNumberField("value", 0)
            gen.writeEndObject()

            gen.writeFieldName("notary")
            gen.writeStartObject()
            gen.writeObjectField("owningKey", emptyPk)
            gen.writeEndObject()
            gen.writeEndObject()

            gen.writeFieldName("ref")
            gen.writeStartObject()
            gen.writeObjectField("id", emptyHash)
            gen.writeEndObject()

            gen.writeEndObject()
        }
    },
    PublicKey {
        override fun default(gen: JsonGenerator) {
            val emptyPk = IntArray(44) { 0 }
            gen.writeObject(emptyPk)
        }
    };

    abstract fun default(gen: JsonGenerator)
}

@JsonSerialize(using = StatusSerializer::class)
private interface StatusMixin

private class StatusSerializer<T> : JsonSerializer<Status<T>>() {
    override fun serialize(value: Status<T>, gen: JsonGenerator, serializers: SerializerProvider) {
        value.serialize(gen)
    }
}

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

@JsonSerialize(using = CommandDataZincSerializer::class)
private interface CommandDataMixinZinc

private class CommandDataZincSerializer : JsonSerializer<CommandData>() {
    override fun serialize(value: CommandData, gen: JsonGenerator, serializers: SerializerProvider) {
        val cmd = when (value) {
            is TestContract.Create -> 0
            is TestContract.Move -> 1
            else -> 3
        }
        gen.writeString("$cmd")
    }
}
