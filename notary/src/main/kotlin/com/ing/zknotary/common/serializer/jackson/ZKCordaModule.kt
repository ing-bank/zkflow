package com.ing.zknotary.common.serializer.jackson

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.ing.zknotary.common.serializer.SerializationFactoryService
import com.ing.zknotary.common.states.ZKStateAndRef
import com.ing.zknotary.common.transactions.ZKProverTransaction
import net.corda.client.jackson.internal.readValueAs
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.PrivacySalt
import net.corda.core.contracts.TimeWindow
import net.corda.core.crypto.DigestService
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.SerializationFactory

class ZKCordaModule : SimpleModule("corda-core") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)

        context.setMixInAnnotations(ZKProverTransaction::class.java, ZKProverTransactionMixin::class.java)
        context.setMixInAnnotations(SerializationFactory::class.java, SerializationFactoryMixin::class.java)
        context.setMixInAnnotations(
            SerializationFactoryService::class.java,
            SerializationFactoryServiceMixin::class.java
        )
        context.setMixInAnnotations(DigestService::class.java, DigestServiceMixin::class.java)
    }
}

@JsonSerialize(using = ZKProverTransactionSerializer::class)
@JsonDeserialize(using = ZKProverTransactionDeserializer::class)
private interface ZKProverTransactionMixin

private class ZKProverTransactionSerializer : JsonSerializer<ZKProverTransaction>() {
    override fun serialize(value: ZKProverTransaction, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeObject(
            ZKProverTransactionJson(
                value.id,
                value.inputs,
                value.outputs,
                value.references,
                value.commands,
                value.notary,
                value.timeWindow,
                value.privacySalt,
                value.networkParametersHash,
                value.attachments,
                value.serializationFactoryService,
                value.componentGroupLeafDigestService,
                value.nodeDigestService
            )
        )
    }
}

private class ZKProverTransactionDeserializer : JsonDeserializer<ZKProverTransaction>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): ZKProverTransaction {
        val wrapper = parser.readValueAs<ZKProverTransactionJson>()
        return ZKProverTransaction(
            wrapper.inputs,
            wrapper.outputs,
            wrapper.references,
            wrapper.commands,
            wrapper.notary,
            wrapper.timeWindow,
            wrapper.privacySalt,
            wrapper.networkParametersHash,
            wrapper.attachments,
            wrapper.serializationFactoryService,
            wrapper.componentGroupLeafDigestService,
            wrapper.nodeDigestService
        )
    }
}

private class ZKProverTransactionJson(
    val id: SecureHash,
    val inputs: List<ZKStateAndRef<ContractState>>,
    val outputs: List<ZKStateAndRef<ContractState>>,
    val references: List<ZKStateAndRef<ContractState>>,
    val commands: List<Command<*>>,
    val notary: Party?,
    val timeWindow: TimeWindow?,
    val privacySalt: PrivacySalt,
    val networkParametersHash: SecureHash?,
    val attachments: List<SecureHash>,
    @get:JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    val serializationFactoryService: SerializationFactoryService,
    @get:JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    val componentGroupLeafDigestService: DigestService,
    @get:JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    val nodeDigestService: DigestService
)

@JsonIgnoreProperties(ignoreUnknown = true, value = ["factory", "serviceHub", "token"])
private interface SerializationFactoryServiceMixin

@JsonIgnoreProperties(ignoreUnknown = true, value = ["defaultContext", "currentContext"])
private interface SerializationFactoryMixin

@JsonIgnoreProperties(ignoreUnknown = true, value = ["allOnesHash", "zeroHash", "digestLength"])
private interface DigestServiceMixin
