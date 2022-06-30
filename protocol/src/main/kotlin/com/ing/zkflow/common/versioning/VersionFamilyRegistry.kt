package com.ing.zkflow.common.versioning

import net.corda.core.contracts.ContractState
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Main API to access the [VersionFamilyRegistry], where the list of [VersionFamily] is read from the [ServiceLoader].
 */
object ContractStateVersionFamilyRegistry :
    VersionFamilyRegistry(VersionFamiliesFromServiceLoaderRetriever)

/**
 * Registry of [VersionFamily] classes. This offers an API for developers to access [VersionFamily] data.
 * This is an open class in order to enhance testability.
 */
open class VersionFamilyRegistry(
    private val retriever: VersionFamiliesRetriever
) {

    private val familyClassToFamily: Map<KClass<*>, VersionFamily> by lazy {
        retriever.families.associateBy { it.familyClass }
    }

    operator fun get(familyKClass: KClass<out VersionedContractStateGroup>): VersionFamily = familyClassToFamily[familyKClass]
        ?: error("Not registered as family: $familyKClass")

    fun familyOf(memberKClass: KClass<out ContractState>): VersionFamily =
        retriever.families.singleOrNull { it.hasMember(memberKClass) }
            ?: error("Not registered as family member: $memberKClass")
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
