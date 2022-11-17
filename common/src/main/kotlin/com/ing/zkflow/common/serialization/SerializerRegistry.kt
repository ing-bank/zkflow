@file:Suppress("DEPRECATION") // For AutomaticHashConstraint

package com.ing.zkflow.common.serialization

import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.network.ZKNetworkParameters
import com.ing.zkflow.serialization.infra.SerializerRegistryError
import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticHashConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticPlaceholderConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.WhitelistedByZoneAttachmentConstraintSerializer
import com.ing.zkflow.util.requireNotNull
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
import java.util.ServiceLoader
import kotlin.reflect.KClass

abstract class SerializerRegistry<T : Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KClassSerializer<T>>()

    init {
        log.trace("Adding available serializers to ${this::class}")
        getAllKClassSerializerProviders().map { it.get() }
            .also { if (it.isEmpty()) log.debug("No serializers found") }
            .forEach { (forKClass, id, serializer) ->
                @Suppress("UNCHECKED_CAST")
                if (forKClass as? KClass<T> != null) {
                    log.trace("Registering serializer for $forKClass")
                    serializer as KSerializer<T>
                    register(KClassSerializer(forKClass, id, serializer))
                }
            }
    }

    /**
     * Register a class and associated serializer
     *
     * Note that both parameters have a 'out' variance annotation on T.
     * This is because we want to allow registering implementations of T, not just T itself. Example would be
     * a SerializerRegistry<AttachmentConstraint> where we register a HashAttachmentConstraint. This would fail without the
     * variance annotation.
     *
     * Also note that when we retrieve the serializer for class, we don't actually care that it is a serializer for the implementation of T
     * or for T. And we can know for sure that it is a T. This is why we can safely cast to T without the variance annotation on retrieval.
     */
    @Synchronized
    fun register(klassSerializer: KClassSerializer<T>) {
        val klass = klassSerializer.klass
        val serializer = klassSerializer.serializer
        val id = klassSerializer.id
        log.trace("Registering serializer `$serializer` under id `$id` for `${klass.qualifiedName}`")

        obj2Id.put(klass, id)?.let { throw SerializerRegistryError.ClassAlreadyRegistered(klass, it) }
        objId2Serializer.put(id, klassSerializer)?.let {
            throw SerializerRegistryError.IdAlreadyRegistered(id, klass, serializer.descriptor.serialName)
        }
    }

    fun identify(klass: KClass<*>): Int = obj2Id[klass] ?: throw SerializerRegistryError.ClassNotRegistered(klass)

    /**
     * Note that when we retrieve the serializer for class, we don't actually care that it is a serializer for the implementation of T
     * or for T. And we can know for sure that it is a T. This is why we can safely cast to T without the variance annotation on retrieval.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(id: Int): KSerializer<T> =
        (objId2Serializer[id]?.serializer ?: throw SerializerRegistryError.ClassNotRegistered(id)) as KSerializer<T>

    operator fun get(klass: KClass<out T>): KSerializer<T> = get(identify(klass))

    fun getKClass(id: Int): KClass<out T> = objId2Serializer[id]?.klass
        ?: throw SerializerRegistryError.ClassNotRegistered(id)
}

/**
 * Returns a list of all KClassSerializerProviders found through the ServiceLoader.
 */
fun getAllKClassSerializerProviders(): List<KClassSerializerProvider> =
    ServiceLoader.load(KClassSerializerProvider::class.java).toList()

object ContractStateSerializerRegistry : SerializerRegistry<ContractState>()
object CommandDataSerializerRegistry : SerializerRegistry<CommandData>()

/**
 * Register a class and associated serializer
 *
 * Note that both parameters have a 'out' variance annotation on T.
 * This is because we want to allow registering implementations of T, not just T itself. Example would be
 * a SerializerRegistry<AttachmentConstraint> where we register a HashAttachmentConstraint. This would fail without the
 * variance annotation.
 *
 * Also note that when we retrieve the serializer for class, we don't actually care that it is a serializer for the implementation of T
 * or for T. And we can know for sure that it is a T. This is why we can safely cast to T without the variance annotation on retrieval.
 */
object AttachmentConstraintSerializerRegistry {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out AttachmentConstraint>, Int>()
    private val objId2Serializer = mutableMapOf<Int, GetAttachmentConstraintSerializer>()

    init {
        register(AlwaysAcceptAttachmentConstraint::class) { AlwaysAcceptAttachmentConstraintSerializer }
        register(HashAttachmentConstraint::class) { zkNetworkParameters ->
            (zkNetworkParameters.attachmentConstraintType as? ZKAttachmentConstraintType.HashAttachmentConstraintType)
                ?.let { HashAttachmentConstraintSerializer(it.digestAlgorithm) }
                ?: error(
                    """
                    Cannot serialize attachment constraint `${HashAttachmentConstraint::class.qualifiedName}`,
                    because expected constraint type is ${zkNetworkParameters.attachmentConstraintType::class.qualifiedName} 
                    """.trimIndent()
                )
        }
        register(WhitelistedByZoneAttachmentConstraint::class) { WhitelistedByZoneAttachmentConstraintSerializer }
        register(AutomaticHashConstraint::class) { AutomaticHashConstraintSerializer }
        register(AutomaticPlaceholderConstraint::class) { AutomaticPlaceholderConstraintSerializer }
        register(SignatureAttachmentConstraint::class) { networkParameters ->
            getSignatureSchemeIdForSignatureAttachmentConstraint(networkParameters.attachmentConstraintType)?.let { signatureSchemeId ->
                SignatureAttachmentConstraintSerializer(signatureSchemeId)
            } ?: throw IllegalArgumentException(
                "Insufficient metadata to construct ${SignatureAttachmentConstraint::class.qualifiedName} "
            )
        }
    }

    @Synchronized
    private fun register(klass: KClass<out AttachmentConstraint>, generator: GetAttachmentConstraintSerializer) {
        log.trace("Registering generator for $klass")

        val id = klass.stableId
        obj2Id.put(klass, id)?.let { throw SerializerRegistryError.ClassAlreadyRegistered(klass, it) }

        objId2Serializer.put(id, generator)?.let { throw SerializerRegistryError.IdAlreadyRegistered(id, klass, null) }
    }

    fun identify(klass: KClass<*>): Int =
        obj2Id[klass] ?: throw SerializerRegistryError.ClassNotRegistered(klass)

    /**
     * Note that when we retrieve the serializer for class, we don't actually care that it is a serializer for the implementation of T
     * or for T. And we can know for sure that it is a T. This is why we can safely cast to T without the variance annotation on retrieval.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(id: Int): (networkParameters: ZKNetworkParameters) -> KSerializer<AttachmentConstraint> =
        { networkParameters ->
            (objId2Serializer[id] ?: throw SerializerRegistryError.ClassNotRegistered(id)).invoke(networkParameters) as KSerializer<AttachmentConstraint>
        }

    operator fun get(attachmentConstraintKClass: KClass<out AttachmentConstraint>): (networkParameters: ZKNetworkParameters) -> KSerializer<AttachmentConstraint> =
        get(identify(attachmentConstraintKClass))

    private fun getSignatureSchemeIdForSignatureAttachmentConstraint(attachmentConstraintType: ZKAttachmentConstraintType): Int? =
        (attachmentConstraintType as? ZKAttachmentConstraintType.SignatureAttachmentConstraintType)?.signatureScheme?.schemeNumberID
}

/**
 * Generate a stable ID for this class.
 * This should be stable across different -versions of- JVMs.
 * In general hashCode() does not fulfill this requirement, but
 * hashCode() is stable for Strings in Java.
 * See https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html#hashCode()
 */
val KClass<*>.stableId: Int
    get() = qualifiedName.requireNotNull {
        "'$this' local class or a class of an anonymous object is not supported"
    }.hashCode()

fun interface GetAttachmentConstraintSerializer {
    operator fun invoke(zkNetworkParameters: ZKNetworkParameters): KSerializer<out AttachmentConstraint>
}
