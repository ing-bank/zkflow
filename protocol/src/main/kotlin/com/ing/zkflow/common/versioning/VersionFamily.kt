package com.ing.zkflow.common.versioning

import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.serialization.SerializerRegistry
import com.ing.zkflow.util.requireNotEmpty
import net.corda.core.contracts.ContractState
import java.util.ServiceLoader
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * Representation of a VersionFamily.
 */
data class VersionFamily(
    val familyClass: KClass<out Versioned>,
    val members: List<KClass<out ContractState>>,
) {
    init {
        members.requireNotEmpty {
            "Empty families (${familyClass.qualifiedName}) are not allowed"
        }
        members.forEach {
            require(it.allSuperclasses.contains(familyClass)) {
                "$it is not a member of ${familyClass.qualifiedName}"
            }
        }
    }

    val latest: KClass<out ContractState> by lazy {
        members.last()
    }

    fun next(current: KClass<out ContractState>): KClass<out ContractState>? {
        val nextIndex = members.indexOf(current) + 1
        require(nextIndex > 0)
        return if (nextIndex == members.size) {
            null
        } else {
            members[nextIndex]
        }
    }
}

/**
 * Provider of a [VersionFamily]. This interface is used to register version families in [ServiceLoader] files.
 */
interface VersionFamilyProvider {
    fun getFamily(): VersionFamily
}

/**
 * Provider for a list of [VersionFamily]. This interface is used in [VersionFamilyRegistry] to provide the families.
 */
interface VersionFamiliesRetriever {
    val families: List<VersionFamily>
}

/**
 * Instance of [VersionFamiliesRetriever] that returns all [VersionFamily] instances obtained via
 * [VersionFamilyProvider] from [ServiceLoader].
 */
object VersionFamiliesFromServiceLoaderRetriever : VersionFamiliesRetriever {
    override val families: List<VersionFamily> by lazy {
        ServiceLoader.load(VersionFamilyProvider::class.java)
            .map { it.getFamily() }
    }
}

/**
 * Registry of [VersionFamily] classes. This offers an API for developers to access [VersionFamily] data.
 * This is an open class in order to enhance testability.
 */
open class VersionFamilyRegistry(
    private val retriever: VersionFamiliesRetriever,
    private val serializerRegistry: SerializerRegistry<ContractState>,
) {

    private val familyClassToFamily: Map<KClass<*>, VersionFamily> by lazy {
        retriever.families.associateBy { it.familyClass }
    }

    private val relativeClassToFamily: Map<KClass<*>, VersionFamily> by lazy {
        retriever.families
            .flatMap { family ->
                family.members.map {
                    it to family
                }
            }
            .toMap()
    }

    operator fun get(familyKClass: KClass<out Versioned>): VersionFamily = familyClassToFamily[familyKClass]
        ?: throw IllegalStateException("Not a family $familyKClass")

    fun familyOf(relativeKClass: KClass<out ContractState>): VersionFamily = relativeClassToFamily[relativeKClass]
        ?: throw IllegalStateException("Not a family member $relativeKClass")

    fun getIdOfLatest(klass: KClass<out ContractState>): Int = serializerRegistry.identify(familyOf(klass).latest)
}

/**
 * Main API to access the [VersionFamilyRegistry], where the list of [VersionFamily] is read from the [ServiceLoader].
 */
object ContractStateVersionFamilyRegistry : VersionFamilyRegistry(VersionFamiliesFromServiceLoaderRetriever, ContractStateSerializerRegistry)
