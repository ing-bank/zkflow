package com.ing.zknotary.common.serialization.bfl

import kotlin.reflect.KClass

sealed class SerializerMapError(message: String) : IllegalArgumentException(message) {
    class ClassAlreadyRegistered(klass: KClass<*>, id: Int) :
        SerializerMapError("Class ${klass.qualifiedName} has already been registered with id $id")

    class IdAlreadyRegistered(id: Int, klass: KClass<*>) :
        SerializerMapError("Could not register class ${klass.qualifiedName}: id $id was already registered")

    class ClassNotRegistered : SerializerMapError {
        constructor(klass: KClass<*>) : super("No registration for Class ${klass.qualifiedName}")
        constructor(id: Int) : super("No Class registered for id = $id")
    }
}
