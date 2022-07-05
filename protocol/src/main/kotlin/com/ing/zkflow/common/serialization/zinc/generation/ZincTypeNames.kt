package com.ing.zkflow.common.serialization.zinc.generation

import com.ing.zkflow.Surrogate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.serializer
import net.corda.core.internal.objectOrNewInstance
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
        .split(".")
        .filter {
            it.startsWithUppercase()
        }.joinToString("_") { it }

private fun String.startsWithUppercase(): Boolean {
    return this[0].isUpperCase()
}

fun KClass<*>.getSerialDescriptor(): SerialDescriptor {
    val fqName = asClassName()
        .buildFullyDistinguishableClassName(Surrogate.GENERATED_SURROGATE_SERIALIZER_POSTFIX)
        .canonicalName

    val serializer = Class.forName(fqName)
        .kotlin
        .objectOrNewInstance() as KSerializer<*>

    return serializer.descriptor
}

fun ClassName.buildFullyDistinguishableClassName(postfix: String): ClassName {
    var prefixes = emptyList<String>()
    var className = this
    var container = className.enclosingClassName()
    while (container != null) {
        prefixes = listOf(container.simpleName) + prefixes
        className = container
        container = className.enclosingClassName()
    }

    return ClassName(
        packageName,
        listOf(
            prefixes.joinToString(separator = "") { it.capitalize() },
            simpleName,
            postfix
        )
            .filter { it.isNotBlank() }
            .joinToString(separator = "_")
    )
}
