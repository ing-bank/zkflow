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
import kotlin.reflect.KClass

/**
 * Interface to allow registration of serializers through providers.
 * At runtime, all such providers will be picked up and injected to
 * [BFLSerializationScheme]
 */
data class KClassSerializer<out T : Any>(
    val klass: KClass<out T>,
    val id: Int,
    val serializer: KSerializer<out T>
)

interface KClassSerializerProvider<T : Any> {
    fun list(): List<KClassSerializer<T>>
}

interface SurrogateSerializerRegistryProvider : KClassSerializerProvider<Any>

interface ContractStateSerializerRegistryProvider : KClassSerializerProvider<ContractState>
interface CommandDataSerializerRegistryProvider : KClassSerializerProvider<CommandData>

abstract class SerializerRegistry<T : Any> {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val obj2Id = mutableMapOf<KClass<out T>, Int>()
    private val objId2Serializer = mutableMapOf<Int, KSerializer<out T>>()

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
        log.debug("Registering serializer `$serializer` under id `$id` for `${klass.qualifiedName}`")

        obj2Id.put(klass, id)?.let { throw SerializerRegistryError.ClassAlreadyRegistered(klass, it) }
        objId2Serializer.put(id, serializer)?.let {
            throw SerializerRegistryError.IdAlreadyRegistered(id, klass, it.descriptor.serialName)
        }
    }

    @Synchronized
    fun register(klass: KClass<out T>, serializer: KSerializer<out T>) =
        register(KClassSerializer<T>(klass, klass.hashCode(), serializer))

    fun identify(klass: KClass<*>): Int = obj2Id[klass] ?: throw SerializerRegistryError.ClassNotRegistered(klass)

    /**
     * Note that when we retrieve the serializer for class, we don't actually care that it is a serializer for the implementation of T
     * or for T. And we can know for sure that it is a T. This is why we can safely cast to T without the variance annotation on retrieval.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(id: Int): KSerializer<T> = (objId2Serializer[id] ?: throw SerializerRegistryError.ClassNotRegistered(id)) as KSerializer<T>

    operator fun get(klass: KClass<out T>): KSerializer<T> = get(identify(klass))
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
        log.debug("Registering generator for $klass")

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
