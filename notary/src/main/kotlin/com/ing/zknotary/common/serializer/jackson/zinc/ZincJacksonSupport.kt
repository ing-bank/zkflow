package com.ing.zknotary.common.serializer.jackson.zinc

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.corda.client.jackson.JacksonSupport
import net.corda.client.jackson.internal.CordaModule
import net.corda.core.internal.kotlinObjectInstance
import java.util.Date

object ZincJacksonSupport {
    @JvmStatic
    @JvmOverloads
    fun createDefaultMapper(fullParties: Boolean = false): ObjectMapper {
        return configureMapper(JacksonSupport.NoPartyObjectMapper(JsonFactory(), fullParties))
    }

    private fun configureMapper(mapper: ObjectMapper): ObjectMapper {
        return mapper.apply {
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS)

            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            registerModule(
                JavaTimeModule().apply {
                    addSerializer(Date::class.java, DateSerializer)
                }
            )
            registerModule(CordaModule())
            registerModule(
                KotlinModule().apply {
                    setDeserializerModifier(KotlinObjectDeserializerModifier)
                }
            )

            registerModule(ZincModule())
        }
    }

    // ***************************************************************************
    // TODO: We have to copy paste everything below this line, because it is needlesly private in [JacksonSupport]
    // ***************************************************************************
    private object KotlinObjectDeserializerModifier : BeanDeserializerModifier() {
        override fun modifyDeserializer(
            config: DeserializationConfig,
            beanDesc: BeanDescription,
            deserializer: JsonDeserializer<*>
        ): JsonDeserializer<*> {
            val objectInstance = beanDesc.beanClass.kotlinObjectInstance
            return if (objectInstance != null) KotlinObjectDeserializer(objectInstance) else deserializer
        }
    }

    private class KotlinObjectDeserializer<T>(private val objectInstance: T) : JsonDeserializer<T>() {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): T = objectInstance
    }

    private object DateSerializer : JsonSerializer<Date>() {
        override fun serialize(value: Date, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeObject(value.toInstant())
        }
    }
}
