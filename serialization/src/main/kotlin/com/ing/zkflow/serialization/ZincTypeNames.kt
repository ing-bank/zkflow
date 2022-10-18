package com.ing.zkflow.serialization

import com.ing.zkflow.Surrogate
import com.ing.zkflow.util.buildFullyDistinguishableClassName
import com.ing.zkflow.util.objectOrNewInstance
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private val javaClass2ZincTypeNameCache: MutableMap<KClass<*>, String> = mutableMapOf()

val KClass<*>.zincTypeName: String
    get() {
        return javaClass2ZincTypeNameCache.computeIfAbsent(this) {
            it.getSerialDescriptor().zincTypeName
        }
    }

val SerialDescriptor.zincTypeName: String
    get() {
        require(kind == SerialKind.ENUM || kind == StructureKind.CLASS) {
            "Unsupported kind $kind. Only Enums and Classes are supported."
        }
        return internalTypeName
    }

val SerialDescriptor.internalTypeName
    get() = serialName
        .replace(Surrogate.GENERATED_SURROGATE_POSTFIX, "")
        .replace(Surrogate.GENERATED_SURROGATE_ENUM_POSTFIX, "")
        .split(".")
        .filter {
            it.startsWithUppercase()
        }.joinToString("_") { it }

private fun String.startsWithUppercase(): Boolean {
    return this[0].isUpperCase()
}

/**
 * Gets a serial descriptor given a KClass.
 * - attempt to get a serial descriptor as if the class was directly @Serializable
 * - attempt to get a serial descriptor as if a serializable surrogate was generated.
 * - fail
 */
fun KClass<*>.getSerialDescriptor(): SerialDescriptor {
    return try {
        this.serializer().descriptor
    } catch (_: Exception) {
        val fqName = buildFullyDistinguishableClassName(Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX).canonicalName

        val serializer = Class.forName(fqName)
            .kotlin
            .objectOrNewInstance() as KSerializer<*>

        serializer.descriptor
    }
}
