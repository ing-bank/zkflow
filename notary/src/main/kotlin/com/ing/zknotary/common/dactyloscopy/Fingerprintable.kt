package com.ing.zknotary.common.dactyloscopy

import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.javaType

/**
 * Classes implementing this interface provide a deterministic representation of its content.
 *
 * This representation should be identical for objects with the same contents.
 * This should also be true when a new object is instantiated with these same values at a later point in time.
 * e.g. when reconstructed from a database.
 *
 * An example usage is to use the fingerprint as input for the leaf hashes when building a Merkle tree.
 */
interface Fingerprintable {
    fun fingerprint(): ByteArray

    /**
     * Instance of a class implementing the Fingerprintable interface
     * is considered capable of fingerprinting if it implements itself the required functionality.
     *
     * If a class inherits from another class implementing Fingerprintable
     * then the descendant also implements Fingerprintable, but the inherited functionality is only
     * valid for the superclass, not the descendants.
     */
    val isFingerprinting: Boolean
        get() {
            val reflection = this::class
            return reflection.declaredMemberFunctions.any { it.isFingerprinting } ||
                reflection.declaredMemberExtensionFunctions.any { it.isFingerprinting }
        }

    companion object {
        const val name = "fingerprint"
    }
}

val KFunction<*>.isFingerprinting: Boolean
    get() = name.contains(Fingerprintable.name, true) &&
        parameters.count() == 1 &&
        returnType.javaType == ByteArray::class.java

val Method.isFingerprinting: Boolean
    get() = name.contains(Fingerprintable.name, true) &&
        parameterCount == 1 &&
        returnType == ByteArray::class.java

/**
 * Collects all super types (including interfaces) of the class such that
 * the collected super types are whitelisted in FingerprintableTypes.
 */
fun Any.allSuperTypesWithFingerprintExtension(fingerprintableTypes: Set<String>) =
    this::class.allSuperclasses.map { it.qualifiedName }.filter { it in fingerprintableTypes }
