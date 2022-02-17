package com.ing.zkflow.serialization

import kotlin.reflect.KClass

sealed class SerializerMapError(message: String) : IllegalArgumentException(message) {
    class ClassAlreadyRegistered(klass: KClass<*>, id: Int) :
        SerializerMapError("Class ${klass.qualifiedName} has already been registered with id $id")

    class IdAlreadyRegistered(id: Int, klass: KClass<*>, serialName: String?) :
        SerializerMapError("Could not register class ${klass.qualifiedName}: id $id was already registered for $serialName")

    class ClassNotRegistered : SerializerMapError {
        constructor(klass: KClass<*>) : super("No registration for Class ${klass.qualifiedName}")
        constructor(klass: KClass<*>, message: String) : super("No registration for Class ${klass.qualifiedName}. $message")
        constructor(id: Int) : super("No Class registered for id = $id")
    }
}
