package com.ing.zkflow.plugins.serialization.serializingobject

import com.ing.zkflow.ASCII
import com.ing.zkflow.ASCIIChar
import com.ing.zkflow.BigDecimalSize
import com.ing.zkflow.Converter
import com.ing.zkflow.Resolver
import com.ing.zkflow.Size
import com.ing.zkflow.UTF8
import com.ing.zkflow.UTF8Char
import com.ing.zkflow.plugins.serialization.annotationOrNull
import com.ing.zkflow.plugins.serialization.annotationSingleArgOrNull
import com.ing.zkflow.plugins.serialization.extractRootType
import com.ing.zkflow.serialization.serializer.BooleanSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthByteArraySerializer
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthMapSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthSetSerializer
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.LongSerializer
import com.ing.zkflow.serialization.serializer.SurrogateSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializer
import com.ing.zkflow.serialization.serializer.WrappedKSerializerWithDefault
import com.ing.zkflow.serialization.serializer.char.ASCIICharSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthASCIIStringSerializer
import com.ing.zkflow.serialization.serializer.string.FixedLengthUTF8StringSerializer
import org.jetbrains.kotlin.psi.KtTypeReference
import java.math.BigDecimal

/**
 * This object lists all routines for constructing serializing objects given the type of a parameter.
 */
internal object Processors {
    private val native: Map<String, ToSerializingObject> = mapOf(
        //
        //
        // Simple types.
        //
        Boolean::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            TypeSerializingObject.ExplicitType(
                typeRef,
                WrappedKSerializerWithDefault::class,
                Boolean::class.simpleName!!,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Boolean::class.simpleName}>(${BooleanSerializer::class.qualifiedName})"
            }
        },
        //
        Int::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            TypeSerializingObject.ExplicitType(
                typeRef, WrappedKSerializerWithDefault::class, Int::class.simpleName!!, emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Int::class.simpleName}>(${IntSerializer::class.qualifiedName})"
            }
        },
        //
        Long::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            TypeSerializingObject.ExplicitType(
                typeRef, WrappedKSerializerWithDefault::class, Long::class.simpleName!!, emptyList()
            ) { _, outer, _ ->
                "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Long::class.simpleName}>(${LongSerializer::class.qualifiedName})"
            }
        },
        //
        Char::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.ASCIIChar/com.ing.zkflow.annotations.UTF8Char annotation.
            typeRef.annotationOrNull<ASCIIChar>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    typeRef, WrappedKSerializerWithDefault::class, Char::class.simpleName!!, emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${ASCIICharSerializer::class.qualifiedName})"
                }
            }

            typeRef.annotationOrNull<UTF8Char>()?.let {
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    typeRef, WrappedKSerializerWithDefault::class, Char::class.simpleName!!, emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${WrappedKSerializerWithDefault::class.qualifiedName}<${Char::class.simpleName}>(${ASCIICharSerializer::class.qualifiedName})"
                }
            }

            error("Char `${typeRef.text}` must be annotated with either ${UTF8Char::class.simpleName} or ${ASCIIChar::class.simpleName} annotations")
        },
        //
        String::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.ASCII/com.ing.zkflow.annotations.UTF8 annotation.
            typeRef.annotationSingleArgOrNull<ASCII>()?.let { maxLength ->
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    typeRef, FixedLengthASCIIStringSerializer::class, String::class.simpleName!!, emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${FixedLengthASCIIStringSerializer::class.qualifiedName}($maxLength)"
                }
            }

            typeRef.annotationSingleArgOrNull<UTF8>()?.let { maxLength ->
                return@ToSerializingObject TypeSerializingObject.ExplicitType(
                    typeRef, FixedLengthUTF8StringSerializer::class, String::class.simpleName!!, emptyList()
                ) { _, outer, _ ->
                    "object $outer: ${FixedLengthUTF8StringSerializer::class.qualifiedName}($maxLength)"
                }
            }

            error("String `${typeRef.text}` must be annotated with either ${UTF8::class.simpleName} or ${ASCII::class.simpleName} annotations")
        },
        //
        //
        // Generic collections.
        //
        List::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${List::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.singleOrNull()
                ?: error("${FixedLengthListSerializer::class.qualifiedName} requires exactly one child")

            TypeSerializingObject.ExplicitType(
                typeRef, FixedLengthListSerializer::class, List::class.simpleName!!, listOf(item)
            ) { _, outer, inner ->
                val single = inner.singleOrNull() ?: error(" List must have a single parametrizing object")
                "object $outer: ${FixedLengthListSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
            }
        },
        //
        Set::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${Set::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.singleOrNull()
                ?: error("${FixedLengthSetSerializer::class.qualifiedName} requires exactly one child")

            TypeSerializingObject.ExplicitType(
                typeRef, FixedLengthSetSerializer::class, Set::class.simpleName!!, listOf(item)
            ) { _, outer, inner ->
                val single = inner.singleOrNull() ?: error(" Set must have a single parametrizing object")
                "object $outer: ${FixedLengthSetSerializer::class.qualifiedName}<${item.cleanTypeDeclaration}>($maxSize, $single)"
            }
        },
        //
        Map::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${Map::class.simpleName} must be annotated with ${Size::class.simpleName}(max collection size)")

            val key = children.getOrNull(0) ?: error("Map requires a key descriptor")
            val value = children.getOrNull(1) ?: error("Map requires a value descriptor")

            TypeSerializingObject.ExplicitType(
                typeRef, FixedLengthMapSerializer::class, Map::class.simpleName!!, listOf(key, value)
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
        //
        //
        // Specialized collections, i.e., collections of primitive types.
        //
        ByteArray::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${ByteArray::class.simpleName} must be annotated with ${Size::class.simpleName}")

            TypeSerializingObject.ExplicitType(
                typeRef, FixedLengthByteArraySerializer::class, ByteArray::class.simpleName!!, emptyList()
            ) { _, outer, _ ->
                "object $outer: ${FixedLengthByteArraySerializer::class.qualifiedName}($maxSize)"
            }
        },
        //
        // Floating point types.
        //
        BigDecimal::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.BigDecimalSize annotation.
            val (integerPart, fractionPart) = typeRef.annotationOrNull<BigDecimalSize>()?.run {
                val integerPart = valueArguments[0].asElement().text.trim().toInt()
                val fractionPart = valueArguments[1].asElement().text.trim().toInt()
                Pair(integerPart, fractionPart)
            } ?: error("Ill-defined type `${typeRef.text}`. ${BigDecimal::class.simpleName} must be annotated with ${BigDecimalSize::class.simpleName}")

            TypeSerializingObject.ExplicitType(
                typeRef,
                FixedLengthFloatingPointSerializer.BigDecimalSerializer::class,
                BigDecimal::class.simpleName!!,
                emptyList()
            ) { _, outer, _ ->
                "object $outer: ${FixedLengthFloatingPointSerializer.BigDecimalSerializer::class.qualifiedName}($integerPart, $fractionPart)"
            }
        }
    )

    private val rest = ToSerializingObject { typeRef, _ ->
        // Here we process 3rd party classes or own serializable classes.
        // • To process a 3rd type via a surrogate, minimally com.ing.zkflow.annotations.Converter or com.ing.zkflow.annotations.Resolver must be present.
        //   At this step, only conversion will be taken into account, respective surrogate will be wrapped into
        //   a defaulted serializer if required.
        // • Own classes must be verified to have been annotated with a com.ing.zkflow.annotations.ZKP annotation to be serializable.
        with(typeRef) {
            annotationOrNull<Converter<*, *>>()?.let {
                // Surrogate class is the _second_ type argument.
                // Conversion provider is the _first_ and _only_ argument.
                // TODO Ideally surrogate class can be deduced from the conversion provider.
                val surrogate = it.typeArguments.getOrNull(1)?.typeReference?.typeElement
                    ?: error("Cannot resolve surrogate type for $it; expected as the second type argument")
                val conversionProvider = it.valueArguments.single().asElement().text

                Pair(surrogate, conversionProvider)
            }
                ?: annotationOrNull<Resolver<*, *>>()?.let {
                    val surrogate = it.typeArguments.getOrNull(1)?.typeReference?.typeElement
                        ?: error("Cannot resolve surrogate type for $it; expected as the second type argument")
                    val conversionProvider = it.valueArguments[1]!!.asElement().text

                    Pair(surrogate, conversionProvider)
                }
        }?.let {
            Pair(it.first, it.second.replace("::class", "").trim())
        }?.let { (surrogateType, conversionProvider) ->
            // If conversion provider is PRESENT
            // => serialize the class via a surrogate.
            TypeSerializingObject.UserType(typeRef) { self, outer ->
                """
                object $outer: ${SurrogateSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}, ${surrogateType.text.trim()}>(
                    ${surrogateType.extractRootType().type}.serializer(),
                    { $conversionProvider.from(it) }
                )
                """.trimIndent()
            }
        } ?: run {
            // If conversion provider is ABSENT
            // => this class may be own class annotated with com.ing.zkflow.annotations.ZKP, to be checked.
            // REMARK: we have only reference to the current type of a parameter, not the actual class definition,
            // thus we can't directly check on this typeRef if ZKP annotation is present.
            // As of now, any class different from above cases is considered to have been annotated with com.ing.zkflow.annotations.ZKP
            // and thus be serializable. If this assumption is incorrect, a compilation error will be thrown by kotlinx.serialization.

            TypeSerializingObject.UserType(typeRef) { self, outer ->
                val type = self.original.typeElement?.extractRootType()?.type ?: error("Cannot infer type of `${self.original}`")
                "object $outer: ${WrappedKSerializer::class.qualifiedName}<${self.cleanTypeDeclaration}>($type.serializer())"
            }
        }
    }

    fun isNonNative(type: String) = !native.keys.contains(type)

    fun forNativeType(type: String, typeRef: KtTypeReference, children: List<SerializingObject>) =
        native[type]?.invoke(typeRef, children) ?: error("No native processor for `$type`")

    fun forNonNativeType(typeRef: KtTypeReference) = rest(typeRef, emptyList())
}

fun interface ToSerializingObject {
    operator fun invoke(typeRef: KtTypeReference, children: List<SerializingObject>): SerializingObject
}
