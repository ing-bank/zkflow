package com.ing.zknotary.common.serializer.jackson

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.math.BigDecimal
import java.security.cert.CertPath
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal
import net.corda.client.jackson.JacksonSupport
import net.corda.client.jackson.internal.CordaModule
import net.corda.client.jackson.internal.ToStringSerialize
import net.corda.client.jackson.internal.jsonObject
import net.corda.client.jackson.internal.readValueAs
import net.corda.core.CordaOID
import net.corda.core.internal.CertRole
import net.corda.core.internal.isStatic
import net.corda.core.internal.kotlinObjectInstance
import net.corda.core.internal.uncheckedCast
import org.bouncycastle.asn1.x509.KeyPurposeId

object ZKJacksonSupport {
    @JvmStatic
    @JvmOverloads
    fun createDefaultMapper(fullParties: Boolean = false): ObjectMapper {
        return configureMapper(JacksonSupport.NoPartyObjectMapper(JsonFactory(), fullParties))
    }

    private fun configureMapper(mapper: ObjectMapper): ObjectMapper {
        return mapper.apply {
            enable(SerializationFeature.INDENT_OUTPUT)
            enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

            registerModule(JavaTimeModule().apply {
                addSerializer(Date::class.java, DateSerializer)
            })
            registerModule(ZKCordaModule())
            registerModule(CordaModule())
            registerModule(KotlinModule().apply {
                setDeserializerModifier(KotlinObjectDeserializerModifier)
            })

            addMixIn(BigDecimal::class.java, BigDecimalMixin::class.java)
            addMixIn(X500Principal::class.java, X500PrincipalMixin::class.java)
            addMixIn(X509Certificate::class.java, X509CertificateMixin::class.java)
            addMixIn(CertPath::class.java, CertPathMixin::class.java)
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

    @ToStringSerialize
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer::class)
    private interface BigDecimalMixin

    private object DateSerializer : JsonSerializer<Date>() {
        override fun serialize(value: Date, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeObject(value.toInstant())
        }
    }

    @ToStringSerialize
    private interface X500PrincipalMixin

    @JsonSerialize(using = X509CertificateSerializer::class)
    @JsonDeserialize(using = X509CertificateDeserializer::class)
    private interface X509CertificateMixin

    private object X509CertificateSerializer : JsonSerializer<X509Certificate>() {
        val keyUsages = arrayOf(
            "digitalSignature",
            "nonRepudiation",
            "keyEncipherment",
            "dataEncipherment",
            "keyAgreement",
            "keyCertSign",
            "cRLSign",
            "encipherOnly",
            "decipherOnly"
        )

        val keyPurposeIds = KeyPurposeId::class.java
            .fields
            .filter { it.isStatic && it.type == KeyPurposeId::class.java }
            .associateBy({ (it.get(null) as KeyPurposeId).id }, { it.name })

        val knownExtensions = setOf(
            "2.5.29.15",
            "2.5.29.17",
            "2.5.29.18",
            "2.5.29.19",
            "2.5.29.37",
            CordaOID.X509_EXTENSION_CORDA_ROLE
        )

        override fun serialize(value: X509Certificate, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.jsonObject {
                writeNumberField("version", value.version)
                writeObjectField("serialNumber", value.serialNumber)
                writeObjectField("subject", value.subjectX500Principal)
                writeObjectField("publicKey", value.publicKey)
                writeObjectField("issuer", value.issuerX500Principal)
                writeObjectField("notBefore", value.notBefore)
                writeObjectField("notAfter", value.notAfter)
                writeObjectField("cordaCertRole", CertRole.extract(value))
                writeObjectField("issuerUniqueID", value.issuerUniqueID)
                writeObjectField("subjectUniqueID", value.subjectUniqueID)
                writeObjectField(
                    "keyUsage",
                    value.keyUsage?.asList()?.mapIndexedNotNull { i, flag -> if (flag) keyUsages[i] else null })
                writeObjectField("extendedKeyUsage", value.extendedKeyUsage?.map { keyPurposeIds[it] ?: it })
                jsonObject("basicConstraints") {
                    val isCa = value.basicConstraints != -1
                    writeBooleanField("isCA", isCa)
                    if (isCa) {
                        writeObjectField(
                            "pathLength",
                            value.basicConstraints.let { if (it != Int.MAX_VALUE) it else null })
                    }
                }
                writeObjectField("subjectAlternativeNames", value.subjectAlternativeNames)
                writeObjectField("issuerAlternativeNames", value.issuerAlternativeNames)
                writeObjectField("otherCriticalExtensions", value.criticalExtensionOIDs - knownExtensions)
                writeObjectField("otherNonCriticalExtensions", value.nonCriticalExtensionOIDs - knownExtensions)
                writeBinaryField("encoded", value.encoded)
            }
        }
    }

    private class X509CertificateDeserializer : JsonDeserializer<X509Certificate>() {
        private val certFactory = CertificateFactory.getInstance("X.509")
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): X509Certificate {
            val encoded = if (parser.currentToken == JsonToken.START_OBJECT) {
                parser.readValueAsTree<ObjectNode>()["encoded"].binaryValue()
            } else {
                parser.binaryValue
            }
            return certFactory.generateCertificate(encoded.inputStream()) as X509Certificate
        }
    }

    @JsonSerialize(using = CertPathSerializer::class)
    @JsonDeserialize(using = CertPathDeserializer::class)
    private interface CertPathMixin

    private class CertPathSerializer : JsonSerializer<CertPath>() {
        override fun serialize(value: CertPath, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeObject(CertPathWrapper(value.type, uncheckedCast(value.certificates)))
        }
    }

    private class CertPathDeserializer : JsonDeserializer<CertPath>() {
        private val certFactory = CertificateFactory.getInstance("X.509")
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): CertPath {
            val wrapper = parser.readValueAs<CertPathWrapper>()
            return certFactory.generateCertPath(wrapper.certificates)
        }
    }

    private data class CertPathWrapper(val type: String, val certificates: List<X509Certificate>) {
        init {
            require(type == "X.509") { "Only X.509 cert paths are supported" }
        }
    }
}
