package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.plugins.serialization.serializingobject.SerializationSupport

/**
 * Every constructor parameter of a class can be either a true parameter or a property parameter.
 * In the latter case, the parameter requires a sequence of serializing objects.
 */
sealed class ConstructorParameter(val definition: String) {
    abstract val serializingObjects: List<String>

    class Self(definition: String) : ConstructorParameter(definition) {
        override val serializingObjects = emptyList<String>()
    }

    class Serializable(definition: String, support: SerializationSupport) : ConstructorParameter(definition) {
        override val serializingObjects = support.serializingObjectKotlinDefinitions
    }
}
