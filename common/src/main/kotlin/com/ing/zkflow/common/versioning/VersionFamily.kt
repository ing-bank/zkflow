package com.ing.zkflow.common.versioning

import net.corda.core.contracts.ContractState
import kotlin.reflect.KClass

/**
 * Representation of a VersionFamily.
 * For this class to work reliably the [members] must be the sorted list of all members of the family marked by
 * [familyClass]. This is generated by the 'compiler-plugin-ksp' plugin, which will enforce these conditions.
 *
 * @param familyClass The family marker class
 * @param members The sorted classes of all family members, in ascending order
 */
data class VersionFamily(
    val familyClass: KClass<out VersionedContractStateGroup>,
    private val members: List<KClass<out ContractState>>,
) {
    val latest: KClass<out ContractState> by lazy {
        members.last()
    }

    val highestVersionSupported = members.size

    fun supportsVersion(version: Int) = version <= highestVersionSupported

    fun hasMember(member: KClass<out ContractState>): Boolean = member in members

    fun getMember(version: Int) = members[version - 1]

    fun versionOf(member: KClass<out ContractState>): Int {
        val version = members.lastIndexOf(member) + 1
        require(version > 0) {
            "${member.qualifiedName} is not a member of $this"
        }
        return version
    }

    fun next(current: KClass<out ContractState>): KClass<out ContractState>? = members.getOrNull(members.lastIndexOf(current) + 1)
}
