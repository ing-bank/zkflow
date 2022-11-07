package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflBigDecimal
import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflUnit
import com.ing.zinc.bfl.dsl.ArrayBuilder.Companion.array
import com.ing.zinc.bfl.dsl.BigDecimalBuilder.Companion.bigDecimal
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enum
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.string
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.naming.camelToZincSnakeCase
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.internalTypeName
import com.ing.zkflow.serialization.serializer.BigDecimalSizeAnnotation
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.SizeAnnotation
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames

object ZincTypeGenerator {
    fun generate(descriptor: SerialDescriptor): BflType {
        /**
         * We handle nullability as soon as possible, so that we can use our special [option]
         */
        return if (descriptor.isNullable) {
            option {
                innerType = generateBflType(
                    descriptor // see [NullableSerializer]
                        .getElementDescriptorByName("value") // `T` descriptor
                )
            }
        } else {
            generateBflType(descriptor)
        }
    }

    @Suppress("ComplexMethod")
    private fun generateBflType(descriptor: SerialDescriptor): BflType {
        return when (FixedLengthType.tryFromSerialName(descriptor.serialName)) {
            FixedLengthType.SET,
            FixedLengthType.LIST -> createList(descriptor)
            FixedLengthType.MAP -> createMap(descriptor)
            FixedLengthType.BYTE_ARRAY -> byteArray(descriptor.getAnnotation<SizeAnnotation>().value)
            FixedLengthType.ASCII_STRING -> string(descriptor.getAnnotation<SizeAnnotation>().value, "Ascii")
            FixedLengthType.UTF8_STRING -> string(descriptor.getAnnotation<SizeAnnotation>().value)
            FixedLengthType.UTF16_STRING -> string(descriptor.getAnnotation<SizeAnnotation>().value, "Utf16")
            FixedLengthType.UTF32_STRING -> string(descriptor.getAnnotation<SizeAnnotation>().value, "Utf32")
            FixedLengthType.BOOLEAN -> BflPrimitive.Bool
            FixedLengthType.BYTE -> createSignedInteger(Byte.SIZE_BITS)
            FixedLengthType.SHORT -> createSignedInteger(Short.SIZE_BITS)
            FixedLengthType.INT -> createSignedInteger(Int.SIZE_BITS)
            FixedLengthType.LONG -> createSignedInteger(Long.SIZE_BITS)
            FixedLengthType.EXACT_LIST -> createArray(descriptor)
            null -> when (descriptor.kind) {
                SerialKind.ENUM -> createEnum(descriptor)
                StructureKind.CLASS -> createClass(descriptor)
                else -> throw IllegalArgumentException("No handler found for ${descriptor.kind}: ${descriptor.serialName}.")
            }
        }
    }

    private fun createSignedInteger(bits: Int) = BflPrimitive.fromIdentifier("i$bits")

    private fun createEnum(serialDescriptor: SerialDescriptor) = enum {
        name = serialDescriptor.internalTypeName
        for (elementIndex in 0 until serialDescriptor.elementsCount) {
            variant(serialDescriptor.getElementName(elementIndex))
        }
    }

    private fun createClass(descriptor: SerialDescriptor): BflModule = if (descriptor.isBigDecimalDescriptor()) {
        createBigDecimal(descriptor)
    } else if (descriptor.elementsCount > 0) {
        createStruct(descriptor)
    } else {
        BflUnit
    }

    internal fun SerialDescriptor.isBigDecimalDescriptor(): Boolean =
        serialName == FixedLengthFloatingPointSerializer.FLOAT ||
            serialName == FixedLengthFloatingPointSerializer.DOUBLE ||
            serialName.startsWith(FixedLengthFloatingPointSerializer.BIG_DECIMAL_PREFIX)

    private fun createStruct(descriptor: SerialDescriptor) = struct {
        name = descriptor.internalTypeName
        (0 until descriptor.elementsCount).mapNotNull { elementIndex ->
            field {
                name = descriptor.getElementName(elementIndex).camelToZincSnakeCase()
                type = generate(descriptor.getElementDescriptor(elementIndex))
            }
        }
    }

    private fun createBigDecimal(descriptor: SerialDescriptor): BflBigDecimal {
        val annotation = descriptor.getAnnotation<BigDecimalSizeAnnotation>()
        return bigDecimal {
            integerSize = annotation.integerSize
            fractionSize = annotation.fractionSize
            name = descriptor.serialName
        }
    }

    private fun createList(descriptor: SerialDescriptor) = list {
        capacity = descriptor.getAnnotation<SizeAnnotation>().value
        elementType = generate(
            descriptor // See [FixedLengthCollectionSerializer]
                .getElementDescriptorByName("values") // `List<T>` descriptor
                .getElementDescriptorByName("0")
        )
    }

    private fun createArray(descriptor: SerialDescriptor) = array {
        capacity = descriptor.getAnnotation<SizeAnnotation>().value
        elementType = generate(
            descriptor // See [ExactLengthCollectionSerializer]
                .getElementDescriptorByName("values") // `List<T>` descriptor
                .getElementDescriptorByName("0")
        )
    }

    private fun createMap(descriptor: SerialDescriptor) = map {
        capacity = descriptor.getAnnotation<SizeAnnotation>().value
        keyType = generate(
            descriptor // See [FixedLengthMapSerializer]
                .getElementDescriptorByName("values") // `MapEntry<K, V>` descriptor
                .getElementDescriptorByName("0")
                .getElementDescriptorByName("first") // `K` descriptor
        )
        valueType = generate(
            descriptor // See [FixedLengthMapSerializer]
                .getElementDescriptorByName("values") // `MapEntry<K, V>` descriptor
                .getElementDescriptorByName("0")
                .getElementDescriptorByName("second") // `V` descriptor
        )
    }
}

internal fun SerialDescriptor.getIndexOfElementName(name: String): Int {
    return (0 until elementsCount).find {
        getElementName(it) == name
    } ?: throw IllegalArgumentException(
        "Element with name `$name` not found in ${
        elementNames.joinToString(
            prefix = "[",
            separator = ", ",
            postfix = "]"
        ) { it }
        }"
    )
}

internal fun SerialDescriptor.getElementDescriptorByName(name: String): SerialDescriptor {
    return getElementDescriptor(getIndexOfElementName(name))
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T : Annotation> SerialDescriptor.findAnnotation(): T? =
    annotations.singleOrNull { it is T } as T?

internal inline fun <reified T : Annotation> SerialDescriptor.getAnnotation(): T = requireNotNull(findAnnotation()) {
    "Annotation ${T::class} not found on $serialName"
}
