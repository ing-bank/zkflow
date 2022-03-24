@file:Suppress("DEPRECATION")

package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.SerdeLogger
import com.ing.zkflow.Surrogate
import com.ing.zkflow.annotations.ASCII
import com.ing.zkflow.annotations.ASCIIChar
import com.ing.zkflow.annotations.BigDecimalSize
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.UTF8Char
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.corda.Algorithm
import com.ing.zkflow.annotations.corda.CordaX500NameSpec
import com.ing.zkflow.annotations.corda.SHA256DigestAlgorithm
import com.ing.zkflow.annotations.corda.SignatureSpec
import com.ing.zkflow.plugins.serialization.serializingobject.SerializingObject
import com.ing.zkflow.plugins.serialization.serializingobject.Tracker
import com.ing.zkflow.plugins.serialization.serializingobject.TypeSerializingObject
import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.BooleanSerializer
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.InstantSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.SerializerWithDefault
import com.ing.zkflow.serialization.serializer.ShortSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.UByteSerializer
import com.ing.zkflow.serialization.serializer.UIntSerializer
import com.ing.zkflow.serialization.serializer.ULongSerializer
import com.ing.zkflow.serialization.serializer.UShortSerializer
import com.ing.zkflow.serialization.serializer.UUIDSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializer
import com.ing.zkflow.serialization.serializer.WrappedFixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import com.ing.zkflow.serialization.serializer.corda.AlwaysAcceptAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticHashConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.AutomaticPlaceholderConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.HashAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.SignatureAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.corda.WhitelistedByZoneAttachmentConstraintSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticHashConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.jetbrains.kotlin.psi.KtValueArgument
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant
import java.util.UUID

/**
 * This object
 * - ties together types and a generic functionality to generate appropriate serializing objects.
 * - offers convenient methods to access generated serializing objects for a given type reference.
 */
internal object Processors {
    /**
     * Processors of the types that are not decomposable any further.
     * I.e., "simple types" such as Boolean, Int, String, etc.,
     * and specialized collection such as ByteArray, IntArray, etc.
     */
    private val standardTypes: Map<String, ToSerializingObject> = mapOf(
        //
        //
        // Simple types.
        //
        Boolean::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Boolean::class.simpleName}>(${BooleanSerializer::class.qualifiedName})"
            }
        },
        //
        Byte::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Byte::class.simpleName}>(${ByteSerializer::class.qualifiedName})"
            }
        },
        //
        UByte::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${UByte::class.simpleName}>(${UByteSerializer::class.qualifiedName})"
            }
        },
        //
        Short::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Short::class.simpleName}>(${ShortSerializer::class.qualifiedName})"
            }
        },
        //
        UShort::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${UShort::class.simpleName}>(${UShortSerializer::class.qualifiedName})"
            }
        },
        //
        Int::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Int::class.simpleName}>(${IntSerializer::class.qualifiedName})"
            }
        },
        //
        UInt::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${UInt::class.simpleName}>(${UIntSerializer::class.qualifiedName})"
            }
        },
        //
        Long::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Long::class.simpleName}>(${LongSerializer::class.qualifiedName})"
            }
        },
        //
        ULong::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedFixedLengthKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${ULong::class.simpleName}>(${ULongSerializer::class.qualifiedName})"
            }
        },
        //
        Char::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.ASCIIChar/com.ing.zkflow.annotations.UTF8Char annotation.
            contextualizedOriginal.findAnnotation<ASCIIChar>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    WrappedFixedLengthKSerializerWithDefault::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${ASCIICharSerializer::class.qualifiedName})"
                }
            }

            contextualizedOriginal.findAnnotation<UTF8Char>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    WrappedFixedLengthKSerializerWithDefault::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${UTF8CharSerializer::class.qualifiedName})"
                }
            }

            error("Char `${contextualizedOriginal.ktTypeReference.text}` must be annotated with either ${UTF8Char::class.simpleName} or ${ASCIIChar::class.simpleName} annotations")
        },
        //
        String::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.ASCII/com.ing.zkflow.annotations.UTF8 annotation.
            contextualizedOriginal.annotationSingleArgument<ASCII>()?.let { maxLength ->
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    FixedLengthASCIIStringSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${FixedLengthASCIIStringSerializer::class.qualifiedName}($maxLength)"
                }
            }

            contextualizedOriginal.annotationSingleArgument<UTF8>()?.let { maxLength ->
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    FixedLengthUTF8StringSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${FixedLengthUTF8StringSerializer::class.qualifiedName}($maxLength)"
                }
            }

            error("String `${contextualizedOriginal.ktTypeReference.text}` must be annotated with either ${UTF8::class.simpleName} or ${ASCII::class.simpleName} annotations")
        },
        //
        //
        // Specialized collections, i.e., collections of primitive types.
        //
        ByteArray::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSizeArgument = contextualizedOriginal.annotationSingleArgument<Size>()
                ?: error("Ill-defined type `${contextualizedOriginal.ktTypeReference.text}`. ${ByteArray::class.simpleName} must be annotated with ${Size::class.simpleName}")

            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                FixedLengthByteArraySerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${FixedLengthByteArraySerializer::class.qualifiedName}($maxSizeArgument)"
            }
        }
        //
        // Floating point types.
        //
    )

    /**
     * Generic collections for which serializing objects can be constructed.
     */
    internal val genericCollections: Map<String, ToSerializingObject> = mapOf(
        List::class.simpleName!! to ToSerializingObject { contextualizedOriginal, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = contextualizedOriginal.annotationSingleArgument<Size>()?.toInt()
                ?: error("Ill-defined type `${contextualizedOriginal.ktTypeReference.text}`. ${List::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.singleOrNull()
                ?: error("${FixedLengthListSerializer::class.qualifiedName} requires exactly one child")

            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                FixedLengthListSerializer::class,
                listOf(item)
            ) { _, outer, inner ->
                val single = inner.singleOrNull() ?: error(" List must have a single parametrizing object")
                "object $outer: ${FixedLengthListSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
            }
        },
        //
        Set::class.simpleName!! to ToSerializingObject { contextualizedOriginal, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = contextualizedOriginal.annotationSingleArgument<Size>()?.toInt()
                ?: error("Ill-defined type `${contextualizedOriginal.ktTypeReference.text}`. ${Set::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.singleOrNull()
                ?: error("${FixedLengthSetSerializer::class.qualifiedName} requires exactly one child")

            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                FixedLengthSetSerializer::class,
                listOf(item)
            ) { _, outer, inner ->
                val single = inner.singleOrNull() ?: error(" Set must have a single parametrizing object")
                "object $outer: ${FixedLengthSetSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
            }
        },
        //
        Map::class.simpleName!! to ToSerializingObject { contextualizedOriginal, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = contextualizedOriginal.annotationSingleArgument<Size>()?.toInt()
                ?: error("Ill-defined type `${contextualizedOriginal.ktTypeReference.text}`. ${Map::class.simpleName} must be annotated with ${Size::class.simpleName}(max collection size)")

            val key = children.getOrNull(0) ?: error("Map requires a key descriptor")
            val value = children.getOrNull(1) ?: error("Map requires a value descriptor")

            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                FixedLengthMapSerializer::class,
                listOf(key, value)
            ) { _, outer, inner ->
                val keyTracker = inner.getOrNull(0)
                    ?: error("To describe ${Map::class.qualifiedName}, names for key and value are required")
                val valueTracker = inner.getOrNull(1)
                    ?: error("To describe ${Map::class.qualifiedName}, names for key and value are required")

                """
                object $outer: ${FixedLengthMapSerializer::class.qualifiedName}<${key.cleanTypeDeclaration}, ${value.cleanTypeDeclaration}>(
                    $maxSize, $keyTracker, $valueTracker
                )
                """.trimIndent()
            }
        },
    )

    /**
     * Other types not from Kotlin standard library.
     */
    private val extendedTypes: Map<String, ToSerializingObject> = mapOf(
        BigDecimal::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            // Require com.ing.zkflow.annotations.BigDecimalSize annotation.
            val (integerPart, fractionPart) = contextualOriginal.findAnnotation<BigDecimalSize>()?.run {
                val integerPart = valueArguments[0].asElement().text.trim().toInt()
                val fractionPart = valueArguments[1].asElement().text.trim().toInt()
                Pair(integerPart, fractionPart)
            } ?: error("Ill-defined type `${contextualOriginal.ktTypeReference.text}`. ${BigDecimal::class.simpleName} must be annotated with ${BigDecimalSize::class.simpleName}")

            TypeSerializingObject.ExplicitType(
                contextualOriginal,
                FixedLengthFloatingPointSerializer.BigDecimalSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${FixedLengthFloatingPointSerializer.BigDecimalSerializer::class.qualifiedName}($integerPart, $fractionPart)"
            }
        },

        Instant::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualOriginal,
                InstantSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${Instant::class.qualifiedName}>(${InstantSerializer::class.qualifiedName})"
            }
        },

        UUID::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualOriginal,
                UUIDSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${UUID::class.qualifiedName}>(${UUIDSerializer::class.qualifiedName})"
            }
        },

        /**
         * To serialize [SecureHash], a single annotation annotated with [Algorithm] must be present.
         * Otherwise, recurse to [forOtherType].
         */
        SecureHash::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            contextualOriginal.findDigestAlgorithm()?.let { algorithm ->
                TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    SecureHashSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${SecureHashSerializer::class.qualifiedName}($algorithm)"
                }.also {
                    SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
                }
            } ?: run {
                // SecureHash has no signature specific annotations, recurse to treating it as a generic user type.
                SerdeLogger.log("Re-cursing to default treatment of ${contextualOriginal.ktTypeReference.text}")
                forOtherType(contextualOriginal)
            }
        },

        /**
         * To serialize [PublicKey], a single annotation annotated with [SignatureSpec] must be present,
         * Otherwise, recurse to [forOtherType]
         */
        PublicKey::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            contextualOriginal.findCordaSignatureId()?.let { cordaSignatureId ->
                TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    PublicKeySerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${PublicKeySerializer::class.qualifiedName}($cordaSignatureId)"
                }.also {
                    SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
                }
            } ?: run {
                // PublicKey has no signature specific annotations, recurse to treating it as a generic user type.
                SerdeLogger.log("Re-cursing to default treatment of ${contextualOriginal.ktTypeReference.text}")
                forOtherType(contextualOriginal)
            }
        },

        /**
         * Ban the usage of [AbstractParty].
         */
        AbstractParty::class.qualifiedName!! to ToSerializingObject { _, _ ->
            error(
                """
                Usage of `${AbstractParty::class.qualifiedName}` is not permitted.
                Select either `${AnonymousParty::class.qualifiedName}` or `${Party::class.qualifiedName}`.
                """.trimIndent()
            )
        },

        /**
         * To serialize [AnonymousParty], a single annotation annotated with [SignatureSpec] must be present.
         * Otherwise, recurse to [forOtherType].
         */
        AnonymousParty::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            contextualOriginal.findCordaSignatureId()?.let { cordaSignatureId ->
                TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    AnonymousPartySerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${AnonymousPartySerializer::class.qualifiedName}($cordaSignatureId)"
                }.also {
                    SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
                }
            } ?: run {
                // AnonymousParty has no signature specific annotations, recurse to treating it as a generic user type.
                SerdeLogger.log("Re-cursing to default treatment of ${contextualOriginal.ktTypeReference.text}")
                forOtherType(contextualOriginal)
            }
        },

        /**
         * To serialize [Party], several annotations may be present:
         * - a single annotation annotated with [SignatureSpec]
         * - zero or one annotation annotated with [CordaX500NameSpec]
         * Otherwise, recurse to [forOtherType].
         */
        Party::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            val cordaSignatureId = contextualOriginal.findCordaSignatureId() ?: run {
                // Party has no signature specific annotations, recurse to treating it as a generic user type.
                SerdeLogger.log("Re-cursing to default treatment of ${contextualOriginal.ktTypeReference.text}")
                return@ToSerializingObject forOtherType(contextualOriginal)
            }

            // Look for CordaX500Name specification:
            val nameSpecAnnotations = contextualOriginal.findAnnotation<CordaX500NameSpec<*>>()

            val inner: (Tracker) -> String = if (nameSpecAnnotations == null) {
                //
                // if none, use the default CordaX500NameSerializer
                { tracker -> "object $tracker: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${CordaX500Name::class.qualifiedName}>(${CordaX500NameSerializer::class.qualifiedName})" }
            } else {
                //
                // if single, parse its arguments and create a chain of serializing objects.
                val surrogate = ContextualizedKtTypeReference(
                    nameSpecAnnotations.typeArguments.firstOrNull()?.typeReference
                        ?: error("Cannot resolve surrogate type for $nameSpecAnnotations; expected as the second type argument"),
                    contextualOriginal.typeResolver
                )
                val conversionProvider = surrogate.resolveClass(nameSpecAnnotations.valueArguments.single() as KtValueArgument);

                { tracker: Tracker ->
                    """
                    object $tracker : ${SerializerWithDefault::class.qualifiedName}<${CordaX500Name::class.qualifiedName}>(${tracker.next()}, ${CordaX500NameSerializer::class.qualifiedName}.default)
                    object ${tracker.next()} : ${SurrogateSerializer::class.qualifiedName}<${CordaX500Name::class.qualifiedName}, CordaX500NameSurrogate>(
                        ${surrogate.cleanTypeDeclaration}.serializer(), { ${conversionProvider.asString()}.from(it) }
                    )
                    """.trimIndent()
                }
            }

            TypeSerializingObject.ExplicitType(
                contextualOriginal,
                PartySerializer::class,
                emptyList()
            ) { _, outer, _ ->
                """
                object $outer: ${PartySerializer::class.qualifiedName}($cordaSignatureId, ${outer.next()})
                ${inner(outer.next())} 
                """.trimIndent()
            }.also {
                SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
            }
        },

        /**
         * Ban the usage of [AttachmentConstraint].
         */
        AttachmentConstraint::class.qualifiedName!! to ToSerializingObject { _, _ ->
            error(
                """
                Usage of interface `${AttachmentConstraint::class.qualifiedName}` is not permitted.
                Select one of the classes implementing the interface:
                `${AlwaysAcceptAttachmentConstraint::class.qualifiedName}`
                `${HashAttachmentConstraint::class.qualifiedName}`
                `${WhitelistedByZoneAttachmentConstraint::class.qualifiedName}`
                `${AutomaticHashConstraint::class.qualifiedName}` (deprecated, use `${AutomaticPlaceholderConstraint::class.qualifiedName}`)
                `${AutomaticPlaceholderConstraint::class.qualifiedName}`
                `${SignatureAttachmentConstraint::class.qualifiedName}`
                """.trimIndent()
            )
        },

        /**
         * The following attachment constraints require no further annotations.
         */
        AlwaysAcceptAttachmentConstraint::class.qualifiedName!! to
            buildAttachmentConstraintToSerializingObject<AlwaysAcceptAttachmentConstraint, AlwaysAcceptAttachmentConstraintSerializer>(),
        WhitelistedByZoneAttachmentConstraint::class.qualifiedName!! to
            buildAttachmentConstraintToSerializingObject<WhitelistedByZoneAttachmentConstraint, WhitelistedByZoneAttachmentConstraintSerializer>(),
        AutomaticHashConstraint::class.qualifiedName!! to
            buildAttachmentConstraintToSerializingObject<AutomaticHashConstraint, AutomaticHashConstraintSerializer>(),
        AutomaticPlaceholderConstraint::class.qualifiedName!! to
            buildAttachmentConstraintToSerializingObject<AutomaticPlaceholderConstraint, AutomaticPlaceholderConstraintSerializer>(),

        /**
         * To serialize [HashAttachmentConstraint], a single annotation annotated with [Algorithm] must be present.
         * Otherwise, recurse to [forOtherType].
         */
        HashAttachmentConstraint::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            contextualOriginal.findDigestAlgorithm()?.let { algorithm ->
                TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    HashAttachmentConstraintSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${HashAttachmentConstraintSerializer::class.qualifiedName}($algorithm)"
                }.also {
                    SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
                }
            }
                // HashAttachmentConstraint has no signature specific annotations, default to SHA256.
                ?: TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    HashAttachmentConstraintSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${HashAttachmentConstraintSerializer::class.qualifiedName}(${SHA256DigestAlgorithm::class.qualifiedName}::class)"
                }.also {
                    SerdeLogger.log(
                        """
                        No hashing algorithm for `${HashAttachmentConstraint::class.qualifiedName}` has been found.
                        Defaulting to `${SHA256DigestAlgorithm::class.qualifiedName}`.
                        Type ${contextualOriginal.ktTypeReference.text} processed successfully
                        """.trimIndent()
                    )
                }
        },

        /**
         * To serialize [SignatureAttachmentConstraint], a single annotation annotated with [SignatureSpec] must be present,
         * Otherwise, recurse to [forOtherType]
         */
        SignatureAttachmentConstraint::class.qualifiedName!! to ToSerializingObject { contextualOriginal, _ ->
            contextualOriginal.findCordaSignatureId()?.let { cordaSignatureId ->
                TypeSerializingObject.ExplicitType(
                    contextualOriginal,
                    SignatureAttachmentConstraintSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${SignatureAttachmentConstraintSerializer::class.qualifiedName}($cordaSignatureId)"
                }.also {
                    SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
                }
            } ?: run {
                // SignatureAttachmentConstraint has no signature specific annotations, recurse to treating it as a generic user type.
                SerdeLogger.log("Re-cursing to default treatment of ${contextualOriginal.ktTypeReference.text}")
                forOtherType(contextualOriginal)
            }
        },
    )

    /**
     * Convenience variable joining together all supported types ensuring no uniqueness of the keys.
     */
    private val native: Map<String, ToSerializingObject> by lazy {
        listOf(standardTypes, extendedTypes, genericCollections).fold(mapOf()) { allTypes, theseTypes ->
            require(allTypes.keys.intersect(theseTypes.keys).isEmpty()) {
                "Keys are not unique across different types"
            }

            allTypes + theseTypes
        }
    }

    /**
     * All other types - user types and third party types - must either have surrogates or be ZKP annotated enums.
     */
    private val otherType = ToSerializingObject { contextualOriginal, _ ->
        // Here we process 3rd party classes or own serializable classes.
        // Such classes must have an associated serializer.
        // Currently, we cannot verify whether this is indeed true.
        // We _naively_ expect that there a relevant serializer has been generated by KSP
        // located in `Surrogate.generatedSurrogateSerializerPackageName`
        // named `ClassName${Surrogate.generatedSurrogateSerializerPostfix}`
        with(contextualOriginal) {
            // Check whether the type has an associated surrogate.
            val surrogateSerializerFqName = findSurrogateSerializer()
            if (surrogateSerializerFqName != null) {
                return@with TypeSerializingObject.UserType(this) { self, outer ->
                    """
                    object $outer: ${WrappedFixedLengthKSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}>(
                        $surrogateSerializerFqName,
                        ${self.contextualizedOriginal.rootType.type}::class.java.isEnum
                    )
                    """.trimIndent()
                }
            }

            // This class may be own class annotated with com.ing.zkflow.annotations.ZKP, verify this.
            val errorSerializerAbsentFor: (fqName: String) -> Nothing = {
                error(
                    """
                    Class $it is not serializable;
                    For own classes, annotate it with `${ZKP::class.qualifiedName}`,
                    for 3rd-party classes, introduce an appropriate ${Surrogate::class.qualifiedName}"
                    """.trimIndent()
                )
            }

            // Ensure the class has a ZKP annotation.
            with(contextualOriginal.rootType.bestEffortResolvedType) {
                when (this) {
                    is BestEffortResolvedType.AsIs -> errorSerializerAbsentFor(simpleName)
                    is BestEffortResolvedType.FullyQualified -> findAnnotation<ZKP>() ?: errorSerializerAbsentFor("$fqName")
                    is BestEffortResolvedType.FullyResolved -> findAnnotation<ZKP>() ?: errorSerializerAbsentFor("$fqName")
                }
            }

            TypeSerializingObject.UserType(contextualOriginal) { self, outer ->
                """
                object $outer: ${WrappedFixedLengthKSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}>(
                    ${self.cleanTypeDeclaration}.serializer(),
                    ${self.contextualizedOriginal.rootType.type}::class.java.isEnum
                    )
                """.trimIndent()
            }
        }
    }

    fun isUserType(type: String) = !native.keys.contains(type)

    fun forNativeType(contextualizedOriginal: ContextualizedKtTypeReference, children: List<SerializingObject>) = with(contextualizedOriginal) {
        native[rootType.type]?.invoke(contextualizedOriginal, children) ?: error("No native processor for `${rootType.type}`")
    }

    fun forOtherType(contextualizedOriginal: ContextualizedKtTypeReference) = otherType(contextualizedOriginal, emptyList())

    /**
     * Utility function: Look for a single signature id specification.
     */
    private fun ContextualizedKtTypeReference.findCordaSignatureId(): Int? {
        val metaAnnotations = findMetaAnnotation<SignatureSpec>()
        return when (metaAnnotations.size) {
            0 -> {
                // This KtTypeReference has no signature specific annotations.
                null
            }
            1 -> when (val meta = metaAnnotations.single()) {
                is BestEffortResolvedAnnotation.Instruction -> error(
                    """
                        User defined signature schemes are prohibited.
                        Scheme ${meta.root} has no corresponding Corda signature scheme.
                    """.trimIndent()
                )
                is BestEffortResolvedAnnotation.Compiled<*> -> (meta.annotation as SignatureSpec).cordaSignatureId
            }
            else -> error("Signature spec annotations are not repeatable, got [${metaAnnotations.joinToString(separator = ", ") { it.root }}] (size = ${metaAnnotations.size})")
        }
    }

    /**
     * Utility function: Look for a single digest algorithm specification.
     */
    private fun ContextualizedKtTypeReference.findDigestAlgorithm(): String? {
        val metaAnnotations = findMetaAnnotation<Algorithm>()
        return when (metaAnnotations.size) {
            0 -> {
                // This KtTypeReference has no hash size specific annotations.
                null
            }
            1 -> {
                when (val meta = metaAnnotations.single()) {
                    is BestEffortResolvedAnnotation.Instruction -> {
                        val plaintextClass = meta
                            .annotation
                            .valueArguments
                            .single()
                            .asElement()

                        when (val resolved = resolveClass(plaintextClass)) {
                            is BestEffortResolvedType.FullyQualified -> resolved.fqName.asString()
                            is BestEffortResolvedType.FullyResolved -> resolved.kClass.qualifiedName
                            is BestEffortResolvedType.AsIs -> null
                        }
                    }
                    is BestEffortResolvedAnnotation.Compiled<*> -> (meta.annotation as Algorithm)
                        .digestAlgorithm
                        .qualifiedName
                }?.let { "$it::class" }
            }
            else -> error(
                """
                ${Algorithm::class.qualifiedName} annotations are not repeatable,
                got [${metaAnnotations.joinToString(separator = ", ") { it.root }}] (size = ${metaAnnotations.size})
                """.trimIndent()
            )
        }
    }

    /**
     * Utility function building serializing object for an object [AttachmentConstraint]'s.
     */
    private inline fun <reified A : AttachmentConstraint, reified S : FixedLengthKSerializerWithDefault<out A>> buildAttachmentConstraintToSerializingObject() = ToSerializingObject { contextualOriginal, _ ->
        TypeSerializingObject.ExplicitType(
            contextualOriginal, S::class, emptyList()
        ) { _, outer, _ ->
            "object $outer: ${WrappedFixedLengthKSerializerWithDefault::class.qualifiedName}<${A::class.qualifiedName!!}>(${S::class.qualifiedName})"
        }.also {
            SerdeLogger.log("Type ${contextualOriginal.ktTypeReference.text} processed successfully")
        }
    }
}

fun interface ToSerializingObject {
    operator fun invoke(contextualizedOriginal: ContextualizedKtTypeReference, children: List<SerializingObject>): SerializingObject
}
