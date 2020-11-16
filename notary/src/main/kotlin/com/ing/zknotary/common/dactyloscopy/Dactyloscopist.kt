package com.ing.zknotary.common.dactyloscopy

import com.ing.zknotary.common.util.toIntList
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

object Dactyloscopist {
    private val container = Class.forName("com.ing.zknotary.common.dactyloscopy." + "FingerprintableTypes" + "Kt")
    private val fingerprintableTypes: Map<String, Method>

    private val logger = LoggerFactory.getLogger(Dactyloscopist::class.java)

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

    @Suppress("LongMethod", "ComplexMethod")
    fun identify(item: Any): ByteArray {
        val itemType = item::class.simpleName

        // ► Check whether item **implements itself** the Fingerprintable interface.
        if (item is Fingerprintable && item.isFingerprinting) {
            logger.trace("[$itemType] Direct fingerprint() implementation found")
            val fingerprint = item.fingerprint()
            logger.trace("[$itemType] Fingerprint: ${fingerprint.toIntList()}")
            return fingerprint
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
                logger.trace("[$itemType] fingerprint() extension function found for $itemType")
                val fingerprint = fingerprintableTypes[superTypes.single()]!!.invoke(container, item)!! as ByteArray
                logger.trace("[$itemType] Fingerprint: ${fingerprint.toIntList()}")
                return fingerprint
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

        // Hack because Array<T> is seen as T[]. That means we can't create an extension function that will match.
        if (item::class.java is Iterable<*>) {
            logger.trace("[$itemType] No fingerprint() implementation found, but is Iterable. Fingerprinting its elements.")
            return (item as Iterable<*>).toList().fingerprint()
        } else if (item::class.java.isArray) {
            logger.trace("[$itemType] No fingerprint() implementation found, but is Array. Fingerprinting its elements.")
            /**
             * If it starts with "[", it is a primitive array. See JavaDoc for [Class.getName()].
             **/
            val type = item::class.java.name.last()
            return when (type) {
                'Z' -> (item as BooleanArray).asList()
                'B' -> (item as ByteArray).asList()
                'C' -> (item as CharArray).asList()
                'D' -> (item as DoubleArray).asList()
                'F' -> (item as FloatArray).asList()
                'I' -> (item as IntArray).asList()
                'J' -> (item as LongArray).asList()
                'S' -> (item as ShortArray).asList()
                else -> (item as Array<*>).asList()
            }.fingerprint()
        }

        logger.trace("[$itemType] No fingerprint() implementation found, recursively fingerprinting its members.")
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

        val membersFingerprint = members.map {
            // Why is this working ???
            // without this clause everything breaks
            @Suppress("UNCHECKED_CAST")
            it as KProperty1<Any, *>

            val value = it.get(item)
            require(value != null) { "All internal values must be non-null: ${it.name} of ${reflection.qualifiedName}" }

            identify(value)
        }.fold(ByteArray(0)) { acc, bytes -> acc + bytes }

        logger.trace("[$itemType] Fingerprint: ${membersFingerprint.toIntList()}")
        return membersFingerprint
    }
}

class MultipleFingerprintImplementations(type: String?, superTypes: List<String>) :
    Exception(
        "Cannot decide which extension to use for fingerprinting: ${superTypes.joinToString(", ")}. " +
            "Implement Fingerprintable for $type"
    )

class MustHavePublicMembers(type: String?) :
    Exception("Type with no associated fingerprinting functionality must have public members: $type")
