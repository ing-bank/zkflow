package com.ing.zknotary.ksp.generator.helpers

import com.squareup.kotlinpoet.asTypeName
import kotlin.Suppress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

fun KClass<out Any>.constructorFrom(that: KClass<out Any>): KFunction<Any> =
    constructors.single {
        it.parameters.size == 1 &&
            it.parameters.first().type.asTypeName() == that.asTypeName()
    }

operator fun Any.get(propertyName: String): Any {
    // Special cases to treat list
    if (this is List<*>) {
        // It seems impossible the `size` property of the kotlin.list using reflection.
        if (propertyName == "size") {
            return this.size
        }

        val indexAccess = propertyName.toIntOrNull(10)
        if (indexAccess != null) {
            return this[indexAccess]!!
        }
    }
    @Suppress("UNCHECKED_CAST")
    val thisProperty = this::class.memberProperties.first { it.name == propertyName } as KProperty1<Any, *>
    return thisProperty.get(this) ?: error("Property $propertyName is not found for ${Any::class.simpleName}")
}
