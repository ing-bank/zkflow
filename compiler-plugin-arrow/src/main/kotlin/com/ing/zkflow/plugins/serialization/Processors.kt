package com.ing.zkflow.plugins.serialization

import com.ing.zkflow.ASCII
import com.ing.zkflow.ASCIIChar
import com.ing.zkflow.BigDecimalSize
import com.ing.zkflow.Converter
import com.ing.zkflow.Resolver
import com.ing.zkflow.Size
import com.ing.zkflow.UTF8
import com.ing.zkflow.UTF8Char
import org.jetbrains.kotlin.psi.KtTypeReference
import java.math.BigDecimal

internal object Processors {
    private val native: Map<String, ToSerializingObject> = mapOf(
        // Simple types.
        //
        Boolean::class.simpleName!! to ToSerializingObject { typeRef, _ -> SerializingObject.BOOLEAN(typeRef) },
        //
        Int::class.simpleName!! to ToSerializingObject { typeRef, _ -> SerializingObject.INT(typeRef) },
        //
        Long::class.simpleName!! to ToSerializingObject { typeRef, _ -> SerializingObject.LONG(typeRef) },
        //
        Char::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.ASCIIChar/com.ing.zkflow.annotations.UTF8Char annotation.
            run {
                typeRef.annotationOrNull<ASCIIChar>()?.let { SerializingObject.ASCII_CHAR(typeRef) }
                    ?: typeRef.annotationOrNull<UTF8Char>()?.let { SerializingObject.UTF8_CHAR(typeRef) }
            } ?: error("Char `${typeRef.text}` must be annotated with either ${UTF8Char::class.simpleName} or ${ASCIIChar::class.simpleName} annotations")
        },
        //
        String::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.ASCII/com.ing.zkflow.annotations.UTF8 annotation.
            run {
                typeRef.annotationSingleArgOrNull<ASCII>()?.let { SerializingObject.ASCII_STRING(typeRef, it.toInt()) }
                    ?: typeRef.annotationSingleArgOrNull<UTF8>()?.let { SerializingObject.UTF8_STRING(typeRef, it.toInt()) }
            } ?: error("String `${typeRef.text}` must be annotated with either ${UTF8::class.simpleName} or ${ASCII::class.simpleName} annotations")
        },
        //
        // Generic collections.
        //
        List::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${List::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.single()

            SerializingObject.LIST(typeRef, maxSize, item)
        },
        //
        Set::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${Set::class.simpleName} must be annotated with ${Size::class.simpleName}")
            val item = children.single()

            SerializingObject.SET(typeRef, maxSize, item)
        },
        //
        Map::class.simpleName!! to ToSerializingObject { typeRef, children ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${Map::class.simpleName} must be annotated with ${Size::class.simpleName}(max collection size)")

            val key = children.getOrNull(0) ?: error("Map requires a key descriptor")
            val value = children.getOrNull(1) ?: error("Map requires a value descriptor")

            SerializingObject.MAP(typeRef, maxSize, key, value)
        },
        //
        // Specialized collections.
        //
        ByteArray::class.simpleName!! to ToSerializingObject { typeRef, _ ->
            // Require com.ing.zkflow.annotations.Size annotation.
            val maxSize = typeRef.annotationSingleArgOrNull<Size>()?.toInt()
                ?: error("Ill-defined type `${typeRef.text}`. ${ByteArray::class.simpleName} must be annotated with ${Size::class.simpleName}")

            SerializingObject.BYTE_ARRAY(typeRef, maxSize)
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

            SerializingObject.BIG_DECIMAL(typeRef, integerPart, fractionPart)
        }
    )

    private val rest = ToSerializingObject { typeRef, _ ->
        // Here we process 3rd party classes or own serializable classes.
        // • To process a 3rd type via a surrogate, minimally com.ing.zkflow.annotations.Converter or com.ing.zkflow.annotations.Resolver must be present.
        //   At this step, only conversion will be taken into account, respective surrogate will be wrapped into
        //   a defaulted serializer if required.
        // • Own classes must be verified to have been annotated with a com.ing.zkflow.annotations.ZKP annotation to be serializable.
        val conversion = run {
            typeRef.annotationOrNull<Converter<*, *>>()?.let {
                // Surrogate class is the _second_ type argument.
                // Conversion provider is the _first_ and _only_ argument.
                // TODO Ideally surrogate class can be deduced from the conversion provider.
                val surrogate = it.typeArguments.getOrNull(1)?.typeReference?.typeElement
                    ?: error("Cannot resolve surrogate type for $it; expected as the second type argument")
                val conversionProvider = it.valueArguments.single().asElement().text

                Pair(surrogate, conversionProvider)
            }
                ?: typeRef.annotationOrNull<Resolver<*, *>>()?.let {
                    val surrogate = it.typeArguments.getOrNull(1)?.typeReference?.typeElement
                        ?: error("Cannot resolve surrogate type for $it; expected as the second type argument")
                    val conversionProvider = it.valueArguments[1]!!.asElement().text

                    Pair(surrogate, conversionProvider)
                }
        }?.let {
            Pair(it.first, it.second.replace("::class", "").trim())
        }

        // If conversion provider is:
        // • present, serialize the class via a surrogate.
        // • absent, this class may be own class annotated with com.ing.zkflow.annotations.ZKP, to be checked.
        if (conversion != null) {
            // Class needs to be serialized via a surrogate.
            SerializingObject.SURROGATE(
                typeRef,
                surrogateType = conversion.first,
                conversionProvider = conversion.second
            )
        } else {
            // Possibly own class.
            // As of now, any class different from above cases is considered to have been annotated with com.ing.zkflow.annotations.ZKP
            // and thus be serializable. If this assumption is incorrect, a compilation error will be thrown by kotlinx.serialization.
            // TODO find a way to verify this class has been annotated with com.ing.zkflow.annotations.ZKP.
            SerializingObject.OWN_CLASS(typeRef)
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
