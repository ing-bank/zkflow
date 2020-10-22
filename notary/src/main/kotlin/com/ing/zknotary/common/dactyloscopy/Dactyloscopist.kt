package com.ing.zknotary.common.dactyloscopy

import java.lang.reflect.Method
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

object Dactyloscopist {
    private val container = Class.forName("com.ing.zknotary.common.dactyloscopy." + "FingerprintableTypes" + "Kt")
    private val fingerprintableTypes: Map<String, Method>

    /**
     * Builds the list of types supporting extension function.
     * This approach leverages the knowledge how extension functions
     * are built by Kotlin.
     * see, https://stackoverflow.com/questions/48635210/how-to-obtain-properties-or-function-declared-in-kotlin-extensions-by-java-refle
     */
    init {
        fingerprintableTypes = container.methods
            .filter { it.isFingerprinting }
            .map {
                val receiver = it.parameters[0].type
                receiver.canonicalName to it
            }.toMap()
    }

    fun identify(item: Any): ByteArray {
        // ► Check whether item **implements itself** the Fingerprintable interface.
        if (item is Fingerprintable && item.isFingerprinting) {
            return item.fingerprint()
        }

        // ► Check whether any of `item`'s superclasses implements an interface,
        // or is a type that has been extended with the fingerprinting extension function,
        // such that either of them has been whitelisted in the FingerprinableType file.
        val superTypes = item.allSuperTypesWithFingerprintExtension(fingerprintableTypes.keys)
        when (superTypes.size) {
            0 -> {}
            1 -> {
                // Force non-nullness is OK, because interfaces has been selected from known types
                // and methods are known to return ByteArrays
                return fingerprintableTypes[superTypes.single()]!!.invoke(container, item)!! as ByteArray
            }
            else -> throw MultipleFingerprintImplementations(item::class.qualifiedName, superTypes)
        }

        // ► Otherwise, try to fingerprint `item` via reflection.
        val reflection = item::class

        // Check `item` can be fingerprinted directly.
        // Primitive types take precedence over classes.
        val receiver = reflection.javaPrimitiveType?.canonicalName ?: reflection.java.canonicalName

        // - Try to find method associated with the type to be fingerprinted,
        // - If succeeded, invoke the method,
        // - Cast as ByteArray is safe, because `specimen` contains only fingerprinting methods (`method.isFingerprinting == true`) by design.
        val fingerprint = fingerprintableTypes[receiver]?.invoke(container, item) as? ByteArray
        if (fingerprint != null) {
            return fingerprint
        }

        // ► Fingerprint `item` by a composing fingerprints of its public constituents.
        val members = reflection.memberProperties
            .filter {
                // Take only public fields.
                it.visibility != null && it.visibility == KVisibility.PUBLIC &&
                    // Field must NOT be annotated with NonFingerprintable
                    it.findAnnotation<NonFingerprintable>() == null
            }
            .sortedBy { it.name }

        if (members.isEmpty()) {
            throw MustHavePublicMembers(reflection.qualifiedName)
        }

        return members.map {
            // Why is this working ???
            // without this clause everything breaks
            it as KProperty1<Any, *>

            val value = it.get(item)
            require(value != null) { "All internal values must be non-null: ${it.name} of ${reflection.qualifiedName}" }

            identify(value!!)
        }.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
    }
}

class MultipleFingerprintImplementations(type: String?, superTypes: List<String>) :
    Exception(
        "Cannot decide which extension to use for fingerprinting: ${superTypes.joinToString(", ")}. " +
            "Implement Fingerprintable for $type"
    )

class MustHavePublicMembers(type: String?) :
    Exception("Type with no associated fingerprinting functionality must have public members: $type")