package com.ing.zkflow.zinc.poet.generate

import com.ing.zinc.bfl.BflModule
import com.ing.zinc.bfl.BflPrimitive
import com.ing.zinc.bfl.BflType
import com.ing.zinc.bfl.BflUnit
import com.ing.zinc.bfl.dsl.EnumBuilder.Companion.enum
import com.ing.zinc.bfl.dsl.FieldBuilder.Companion.field
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.asciiString
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.byteArray
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.dsl.ListBuilder.Companion.utf8String
import com.ing.zinc.bfl.dsl.MapBuilder.Companion.map
import com.ing.zinc.bfl.dsl.OptionBuilder.Companion.option
import com.ing.zinc.bfl.dsl.StructBuilder.Companion.struct
import com.ing.zinc.naming.camelToSnakeCase
import com.ing.zkflow.common.serialization.zinc.generation.internalTypeName
import com.ing.zkflow.serialization.FixedLengthType
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
            FixedLengthType.UTF8_STRING -> utf8String(descriptor.getAnnotation<SizeAnnotation>().value)
            FixedLengthType.ASCII_STRING -> asciiString(descriptor.getAnnotation<SizeAnnotation>().value)
            FixedLengthType.BOOLEAN -> BflPrimitive.Bool
            FixedLengthType.BYTE -> createSignedInteger(Byte.SIZE_BITS)
            FixedLengthType.UBYTE -> createUnsignedInteger(UByte.SIZE_BITS)
            FixedLengthType.SHORT -> createSignedInteger(Short.SIZE_BITS)
            FixedLengthType.USHORT -> createUnsignedInteger(UShort.SIZE_BITS)
            FixedLengthType.INT -> createSignedInteger(Int.SIZE_BITS)
            FixedLengthType.UINT -> createUnsignedInteger(UInt.SIZE_BITS)
            FixedLengthType.LONG -> createSignedInteger(Long.SIZE_BITS)
            FixedLengthType.ULONG -> createUnsignedInteger(ULong.SIZE_BITS)
            null -> when (descriptor.kind) {
                SerialKind.ENUM -> createEnum(descriptor)
                StructureKind.CLASS -> createStruct(descriptor)
                else -> throw IllegalArgumentException("No handler found for ${descriptor.kind}: ${descriptor.serialName}.")
            }
        }
    }

    private fun createSignedInteger(bits: Int) = BflPrimitive.fromIdentifier("i$bits")

    private fun createUnsignedInteger(bits: Int) = BflPrimitive.fromIdentifier("u$bits")

    private fun createEnum(serialDescriptor: SerialDescriptor) = enum {
        name = serialDescriptor.internalTypeName
        for (elementIndex in 0 until serialDescriptor.elementsCount) {
            variant(serialDescriptor.getElementName(elementIndex))
        }
    }

    private fun createStruct(descriptor: SerialDescriptor): BflModule {
        val fields = (0 until descriptor.elementsCount).mapNotNull { elementIndex ->
            field {
                name = descriptor.getElementName(elementIndex).camelToSnakeCase()
                type = generate(descriptor.getElementDescriptor(elementIndex))
            }
        }
        return if (fields.isNotEmpty()) {
            struct {
                name = descriptor.internalTypeName
                addFields(fields)
            }
        } else {
            BflUnit
        }
    }

    private fun createList(descriptor: SerialDescriptor) = list {
        capacity = descriptor.getAnnotation<SizeAnnotation>().value
        elementType = generate(
            descriptor // See [FixedLengthListSerializer]
                .getElementDescriptorByName("list") // `List<T>` descriptor
                .getElementDescriptorByName("0")
        )
    }

    private fun createMap(descriptor: SerialDescriptor) = map {
        capacity = descriptor.getAnnotation<SizeAnnotation>().value
        keyType = generate(
            descriptor // See [FixedLengthMapSerializer]
                .getElementDescriptorByName("list") // `MapEntry<K, V>` descriptor
                .getElementDescriptorByName("0")
                .getElementDescriptorByName("first") // `K` descriptor
        )
        valueType = generate(
            descriptor // See [FixedLengthMapSerializer]
                .getElementDescriptorByName("list") // `MapEntry<K, V>` descriptor
                .getElementDescriptorByName("0")
                .getElementDescriptorByName("second") // `V` descriptor
        )
    }
}

private fun SerialDescriptor.getIndexOfElementName(name: String): Int {
    return (0 until elementsCount).find {
        getElementName(it) == name
    } ?: throw IllegalArgumentException(
        "Element with name `$name` not found in ${elementNames.joinToString(
            prefix = "[",
            separator = ", ",
            postfix = "]"
        ) { it }}"
    )
}

private fun SerialDescriptor.getElementDescriptorByName(name: String): SerialDescriptor {
    return getElementDescriptor(getIndexOfElementName(name))
}

@Suppress("UNCHECKED_CAST")
private inline fun <reified T : Annotation> SerialDescriptor.findAnnotation(): T? = annotations.singleOrNull { it is T } as T?

private inline fun <reified T : Annotation> SerialDescriptor.getAnnotation(): T = requireNotNull(findAnnotation()) {
    "Annotation ${T::class} not found on $serialName"
}
