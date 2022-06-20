package com.ing.zkflow.serialization

import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.SerializerRegistry
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

@Synchronized
public fun <T : Any> SerializerRegistry<T>.register(klass: KClass<out T>, serializer: KSerializer<out T>): Unit =
    register(KClassSerializer(klass, klass.hashCode(), serializer))
