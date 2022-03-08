package com.ing.zkflow.serialization.infra

import kotlin.reflect.KClass

sealed class SerializerRegistryError(message: String) : IllegalArgumentException(message) {
    class ClassAlreadyRegistered(klass: KClass<*>, id: Int) :
        SerializerRegistryError("Class ${klass.qualifiedName} has already been registered with id $id")

    class IdAlreadyRegistered(id: Int, klass: KClass<*>, serialName: String?) :
        SerializerRegistryError("Could not register class ${klass.qualifiedName}: id $id was already registered for $serialName")

    class ClassNotRegistered : SerializerRegistryError {
        companion object {
            const val PLEASE_ANNOTATE_MESSAGE = "Please annotate it with @ZKP or annotate a surrogate with @ZKPSurrogate."
        }

        constructor(klass: KClass<*>) : super("No registration for class ${klass.qualifiedName}. $PLEASE_ANNOTATE_MESSAGE")
        constructor(id: Int) : super("No Class registered for id = $id. $PLEASE_ANNOTATE_MESSAGE")
    }
}
