package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.ASCII
import com.ing.zkflow.ASCIIChar
import com.ing.zkflow.BigDecimalSize
import com.ing.zkflow.Converter
import com.ing.zkflow.Resolver
import com.ing.zkflow.Size
import com.ing.zkflow.UTF8
import com.ing.zkflow.UTF8Char
import com.ing.zkflow.plugins.serialization.serializingobject.SerializingObject
import com.ing.zkflow.plugins.serialization.serializingobject.TypeSerializingObject
import com.ing.zkflow.serialization.UUIDSerializer
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
import com.ing.zkflow.serialization.serializer.ShortSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.UByteSerializer
import com.ing.zkflow.serialization.serializer.UIntSerializer
import com.ing.zkflow.serialization.serializer.ULongSerializer
import com.ing.zkflow.serialization.serializer.UShortSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.char.UTF8CharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import org.jetbrains.kotlin.psi.KtValueArgument
import java.math.BigDecimal
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
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Boolean::class.simpleName}>(${BooleanSerializer::class.qualifiedName})"
            }
        },
        //
        Byte::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Byte::class.simpleName}>(${ByteSerializer::class.qualifiedName})"
            }
        },
        //
        UByte::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${UByte::class.simpleName}>(${UByteSerializer::class.qualifiedName})"
            }
        },
        //
        Short::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Short::class.simpleName}>(${ShortSerializer::class.qualifiedName})"
            }
        },
        //
        UShort::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${UShort::class.simpleName}>(${UShortSerializer::class.qualifiedName})"
            }
        },
        //
        Int::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Int::class.simpleName}>(${IntSerializer::class.qualifiedName})"
            }
        },
        //
        UInt::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${UInt::class.simpleName}>(${UIntSerializer::class.qualifiedName})"
            }
        },
        //
        Long::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Long::class.simpleName}>(${LongSerializer::class.qualifiedName})"
            }
        },
        //
        ULong::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                WrappedKSerializerWithDefault::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${ULong::class.simpleName}>(${ULongSerializer::class.qualifiedName})"
            }
        },
        //
        Char::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.ASCIIChar/com.ing.zkflow.annotations.UTF8Char annotation.
            contextualizedOriginal.annotationOrNull<ASCIIChar>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    WrappedKSerializerWithDefault::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${ASCIICharSerializer::class.qualifiedName})"
                }
            }

            contextualizedOriginal.annotationOrNull<UTF8Char>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    WrappedKSerializerWithDefault::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${UTF8CharSerializer::class.qualifiedName})"
                }
            }

            error("Char `${contextualizedOriginal.ktTypeReference.text}` must be annotated with either ${UTF8Char::class.simpleName} or ${ASCIIChar::class.simpleName} annotations")
        },
        //
        String::class.simpleName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.ASCII/com.ing.zkflow.annotations.UTF8 annotation.
            contextualizedOriginal.annotationSingleArgOrNull<ASCII>()?.let { maxLength ->
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    contextualizedOriginal,
                    FixedLengthASCIIStringSerializer::class,
                    emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${FixedLengthASCIIStringSerializer::class.qualifiedName}($maxLength)"
                }
            }

            contextualizedOriginal.annotationSingleArgOrNull<UTF8>()?.let { maxLength ->
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
            val maxSizeArgument = contextualizedOriginal.annotationSingleArgOrNull<Size>()
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
            val maxSize = contextualizedOriginal.annotationSingleArgOrNull<Size>()?.toInt()
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
            val maxSize = contextualizedOriginal.annotationSingleArgOrNull<Size>()?.toInt()
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
            val maxSize = contextualizedOriginal.annotationSingleArgOrNull<Size>()?.toInt()
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
        BigDecimal::class.qualifiedName!! to ToSerializingObject { contextualizedOriginal, _ ->
            // Require com.ing.zkflow.annotations.BigDecimalSize annotation.
            val (integerPart, fractionPart) = contextualizedOriginal.annotationOrNull<BigDecimalSize>()?.run {
                val integerPart = valueArguments[0].asElement().text.trim().toInt()
                val fractionPart = valueArguments[1].asElement().text.trim().toInt()
                Pair(integerPart, fractionPart)
            } ?: error("Ill-defined type `${contextualizedOriginal.ktTypeReference.text}`. ${BigDecimal::class.simpleName} must be annotated with ${BigDecimalSize::class.simpleName}")

            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                FixedLengthFloatingPointSerializer.BigDecimalSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${FixedLengthFloatingPointSerializer.BigDecimalSerializer::class.qualifiedName}($integerPart, $fractionPart)"
            }
        },

        Instant::class.qualifiedName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                InstantSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Instant::class.qualifiedName}>(${InstantSerializer::class.qualifiedName})"
            }
        },

        UUID::class.qualifiedName!! to ToSerializingObject { contextualizedOriginal, _ ->
            TypeSerializingObject.ExplicitType(
                contextualizedOriginal,
                UUIDSerializer::class,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${UUID::class.qualifiedName}>(${UUIDSerializer::class.qualifiedName})"
            }
        }
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
     * All types which are not supported natively are treated as user types.
     */
    private val userType = ToSerializingObject { contextualizedOriginal, _ ->
        // Here we process 3rd party classes or own serializable classes.
        // • To process a 3rd type via a surrogate, minimally com.ing.zkflow.annotations.Converter or com.ing.zkflow.annotations.Resolver must be present.
        //   At this step, only conversion will be taken into account, respective surrogate will be wrapped into
        //   a defaulted serializer if required.
        // • Own classes must be verified to have been annotated with a com.ing.zkflow.annotations.ZKP annotation to be serializable.
        with(contextualizedOriginal) {
            annotationOrNull<Converter<*, *>>()?.let {
                // Surrogate class is the _second_ type argument.
                // Conversion provider is the _first_ and _only_ argument.
                // TODO Ideally surrogate class can be deduced from the conversion provider.
                val surrogate = ContextualizedKtTypeReference(
                    it.typeArguments.getOrNull(1)?.typeReference
                        ?: error("Cannot resolve surrogate type for $it; expected as the second type argument"),
                    typeResolver
                )
                val conversionProvider = it.valueArguments.single() as KtValueArgument

                Pair(surrogate, conversionProvider)
            }
                ?: annotationOrNull<Resolver<*, *>>()?.let {
                    val surrogate = ContextualizedKtTypeReference(
                        it.typeArguments.getOrNull(1)?.typeReference
                            ?: error("Cannot resolve surrogate type for $it; expected as the second type argument"),
                        typeResolver
                    )
                    // it is safe to `!!` because now we're past code validity verification step,
                    // that is, all function signatures are ensured.
                    val conversionProvider = it.valueArguments[1]!! as KtValueArgument

                    Pair(surrogate, conversionProvider)
                }
        }?.let { (surrogate, conversionProviderClass) ->
            Pair(surrogate, surrogate.resolveClass(conversionProviderClass))
        }?.let { (surrogate, conversionProvider) ->
            // If conversion provider is PRESENT
            // => serialize the class via a surrogate.
            TypeSerializingObject.UserType(contextualizedOriginal) { self, outer ->
                """
                object $outer: ${SurrogateSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}, ${surrogate.cleanTypeDeclaration}>(
                    ${surrogate.cleanTypeDeclaration}.serializer(), { ${conversionProvider.type}.from(it) }
                )
                """.trimIndent()
            }
        } ?: run {
            // If conversion provider is ABSENT
            // => this class may be own class annotated with com.ing.zkflow.annotations.ZKP, to be checked.
            // REMARK: we have only a reference to the current type of the parameter, not the actual class definition,
            // thus we can't directly check on this contextualizedOriginal if ZKP annotation is present.
            // As of now, any class different from above cases is considered to have been annotated with com.ing.zkflow.annotations.ZKP
            // and thus be serializable. If this assumption is incorrect, a compilation error will be thrown by kotlinx.serialization.

            TypeSerializingObject.UserType(contextualizedOriginal) { self, outer ->
                "object $outer: ${WrappedKSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}>(${self.cleanTypeDeclaration}.serializer())"
            }
        }
    }

    fun isUserType(type: String) = !native.keys.contains(type)

    fun forNativeType(contextualizedOriginal: ContextualizedKtTypeReference, children: List<SerializingObject>) = with(contextualizedOriginal) {
        native[rootType.type]?.invoke(contextualizedOriginal, children) ?: error("No native processor for `${rootType.type}`")
    }

    fun forUserType(contextualizedOriginal: ContextualizedKtTypeReference) = userType(contextualizedOriginal, emptyList())
}

fun interface ToSerializingObject {
    operator fun invoke(contextualizedOriginal: ContextualizedKtTypeReference, children: List<SerializingObject>): SerializingObject
}
