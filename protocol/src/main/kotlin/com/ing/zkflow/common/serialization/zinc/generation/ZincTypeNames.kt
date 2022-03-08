package com.ing.zkflow.common.serialization.zinc.generation

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private val javaClass2ZincTypeNameCache: MutableMap<KClass<*>, String> = mutableMapOf()

val KClass<*>.zincTypeName: String
    get() {
        return javaClass2ZincTypeNameCache.computeIfAbsent(this) {
            it.serializer().descriptor.zincTypeName
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
    get() = serialName.split(".").filter {
        it.startsWithUppercase()
    }.joinToString("_") { it }

private fun String.startsWithUppercase(): Boolean {
    return this[0].isUpperCase()
}
