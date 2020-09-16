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

    companion object {
        private const val name = "fingerprint"
        fun isFingerprinting(func: KFunction<*>): Boolean =
            func.name.contains(name, true) &&
                func.parameters.count() == 1 &&
                func.returnType.javaType == ByteArray::class.java

        fun isFingerprinting(method: Method): Boolean =
            method.name.contains(name, true) &&
                method.parameterCount == 1 &&
                method.returnType == ByteArray::class.java
    }
}

val KFunction<*>.isFingerprinting: Boolean
    get() = Fingerprintable.isFingerprinting(this)

val Method.isFingerprinting: Boolean
    get() = Fingerprintable.isFingerprinting(this)

/**
 * Instance of a class implementing the Fingerprintable interface
 * is considered capable of fingerprinting if it implements itself the required functionality.
 *
 * The reason for that is that if a class inherits from another class implementing Fingerprintable
 * then the descendant also implements Fingerprintable, but the inherited functionality is only
 * valid for the superclass, not the descendants.
 */
val Fingerprintable.isFingerprinting: Boolean
    get() {
        val reflection = this::class
        return reflection.declaredMemberFunctions.any { it.isFingerprinting } ||
            reflection.declaredMemberExtensionFunctions.any { it.isFingerprinting }
    }

fun Any.allImplementedExtendedInterfaces(interfaceNames: Set<String>) =
    this::class.allSuperclasses.map { it.qualifiedName }.filter { interfaceNames.contains(it) }
