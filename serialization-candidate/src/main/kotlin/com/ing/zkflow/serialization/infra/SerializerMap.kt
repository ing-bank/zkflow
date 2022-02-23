@file:Suppress("DEPRECATION")

package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticHashConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticPlaceholderConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.WhitelistedByZoneAttachmentConstraintSerializer
import kotlinx.serialization.KSerializer
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Interface to allow registration of serializers through providers.
 * At runtime, all such providers will be picked up and injected to
 * [BFLSerializationScheme]
 */
interface ZKDataProvider<T : Any> {
    fun list(): List<Pair<KClass<out T>, KSerializer<out T>>>
}

interface ZKContractStateSerializerMapProvider : ZKDataProvider<ContractState>
interface ZkCommandDataSerializerMapProvider : ZKDataProvider<CommandData>

abstract class SerializerMap<T : Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KSerializer<out T>>()

    fun register(klass: KClass<out T>, serializer: KSerializer<out T>) {
        log.debug("Registering serializer `$serializer` for `${klass.qualifiedName}`")
        val id = klass.hashCode() // Should be deterministic enough

        obj2Id.put(klass, id)?.let { throw SerializerMapError.ClassAlreadyRegistered(klass, it) }
        objId2Serializer.put(id, serializer)?.let {
            throw SerializerMapError.IdAlreadyRegistered(id, klass, it.descriptor.serialName)
        }
    }

    fun tryRegister(klass: KClass<out T>, serializer: KSerializer<out T>) {
        try { register(klass, serializer) } catch (_: SerializerMapError) {
            log.trace("Serializer $serializer has already been registered for `${klass::qualifiedName}`. Skipping.")
        }
    }

    fun identify(klass: KClass<*>): Int =
        obj2Id[klass] ?: throw SerializerMapError.ClassNotRegistered(klass)

    fun retrieve(id: Int): KSerializer<out T> =
        objId2Serializer[id] ?: throw SerializerMapError.ClassNotRegistered(id)

    operator fun get(klass: KClass<*>): KSerializer<out T> =
        obj2Id[klass]?.let { objId2Serializer[it] } ?: throw SerializerMapError.ClassNotRegistered(klass)
}

object AttachmentConstraintSerializerMap {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out AttachmentConstraint>, Int>()
    private val objId2Serializer = mutableMapOf<Int, GetAttachmentConstraintSerializer>()

    init {
        register(AlwaysAcceptAttachmentConstraint::class) { AlwaysAcceptAttachmentConstraintSerializer }
        register(HashAttachmentConstraint::class) { metadata ->
            require(metadata.hashAttachmentConstraintSpec != null) {
                "Insufficient metadata to construct ${HashAttachmentConstraintSerializer::class.qualifiedName} "
            }
            HashAttachmentConstraintSerializer(metadata.hashAttachmentConstraintSpec.algorithm, metadata.hashAttachmentConstraintSpec.hashLength)
        }
        register(WhitelistedByZoneAttachmentConstraint::class) { WhitelistedByZoneAttachmentConstraintSerializer }
        register(AutomaticHashConstraint::class) { AutomaticHashConstraintSerializer }
        register(AutomaticPlaceholderConstraint::class) { AutomaticPlaceholderConstraintSerializer }
        register(SignatureAttachmentConstraint::class) { metadata ->
            require(metadata.signatureAttachmentConstraintSignatureSchemeId != null) {
                "Insufficient metadata to construct ${SignatureAttachmentConstraint::class.qualifiedName} "
            }
            SignatureAttachmentConstraintSerializer(metadata.signatureAttachmentConstraintSignatureSchemeId)
        }
    }

    private fun register(klass: KClass<out AttachmentConstraint>, generator: GetAttachmentConstraintSerializer) {
        log.debug("Registering generator for $klass")

        val id = klass.hashCode()
        obj2Id.put(klass, id)?.let { throw SerializerMapError.ClassAlreadyRegistered(klass, it) }
        objId2Serializer.put(id, generator)?.let {
            throw SerializerMapError.IdAlreadyRegistered(id, klass, null)
        }
    }

    fun identify(klass: KClass<*>): Int =
        obj2Id[klass] ?: throw SerializerMapError.ClassNotRegistered(klass)

    fun retrieve(id: Int): GetAttachmentConstraintSerializer =
        objId2Serializer[id] ?: throw SerializerMapError.ClassNotRegistered(id)

    operator fun get(attachmentConstraintKClass: KClass<out AttachmentConstraint>): GetAttachmentConstraintSerializer =
        obj2Id[attachmentConstraintKClass]?.let { objId2Serializer[it] } ?: throw SerializerMapError.ClassNotRegistered(attachmentConstraintKClass)
}

fun interface GetAttachmentConstraintSerializer {
    operator fun invoke(metadata: AttachmentConstraintMetadata): KSerializer<out AttachmentConstraint>
}
