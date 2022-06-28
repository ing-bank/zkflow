package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.common.serialization.zinc.generation.internalTypeName
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.BigDecimalSizeAnnotation
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.SizeAnnotation
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import com.ing.zkflow.util.tryGetKClass
import com.ing.zkflow.zinc.poet.generate.getAnnotation
import com.ing.zkflow.zinc.poet.generate.getElementDescriptorByName
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

object BflStructureGenerator {
    fun generate(descriptor: SerialDescriptor): BflStructureType {
        /**
         * We handle nullability as soon as possible, so that we can use our special [option]
         */
        return if (descriptor.isNullable) {
            val innerType = generate(
                descriptor // see [NullableSerializer]
                    .getElementDescriptorByName("value") // `T` descriptor
            )
            BflStructureNullable(
                byteSize = innerType.byteSize + 1,
                innerType = innerType,
            )
        } else {
            generateBflType(descriptor)
        }
    }

    @Suppress("ComplexMethod")
    private fun generateBflType(descriptor: SerialDescriptor): BflStructureType {
        return when (FixedLengthType.tryFromSerialName(descriptor.serialName)) {
            FixedLengthType.SET,
            FixedLengthType.LIST -> createList(descriptor)
            FixedLengthType.MAP -> createMap(descriptor)
            FixedLengthType.BYTE_ARRAY -> createArray(descriptor)
            FixedLengthType.ASCII_STRING -> createString(descriptor, "Ascii")
            FixedLengthType.UTF8_STRING -> createString(descriptor, "Utf8")
            FixedLengthType.UTF16_STRING -> createString(descriptor, "Utf16")
            FixedLengthType.UTF32_STRING -> createString(descriptor, "Utf32")
            FixedLengthType.BOOLEAN -> BflStructurePrimitive(Boolean::class.qualifiedName!!, Byte.SIZE_BYTES)
            FixedLengthType.BYTE -> BflStructurePrimitive(Byte::class.qualifiedName!!, Byte.SIZE_BYTES)
            FixedLengthType.UBYTE -> BflStructurePrimitive(UByte::class.qualifiedName!!, UByte.SIZE_BYTES)
            FixedLengthType.SHORT -> BflStructurePrimitive(Short::class.qualifiedName!!, Short.SIZE_BYTES)
            FixedLengthType.USHORT -> BflStructurePrimitive(UShort::class.qualifiedName!!, UShort.SIZE_BYTES)
            FixedLengthType.INT -> BflStructurePrimitive(Int::class.qualifiedName!!, Int.SIZE_BYTES)
            FixedLengthType.UINT -> BflStructurePrimitive(UInt::class.qualifiedName!!, UInt.SIZE_BYTES)
            FixedLengthType.LONG -> BflStructurePrimitive(Long::class.qualifiedName!!, Long.SIZE_BYTES)
            FixedLengthType.ULONG -> BflStructurePrimitive(ULong::class.qualifiedName!!, ULong.SIZE_BYTES)
            FixedLengthType.EXACT_LIST -> createArray(descriptor)
            null -> when (descriptor.kind) {
                SerialKind.ENUM -> createEnum(descriptor)
                StructureKind.CLASS -> createClass(descriptor)
                else -> error("No handler found for ${descriptor.kind}: ${descriptor.serialName}.")
            }
        }
    }

    private fun createEnum(serialDescriptor: SerialDescriptor): BflStructureType =
        BflStructureEnum(serialDescriptor.internalTypeName)

    private fun createClass(descriptor: SerialDescriptor): BflStructureType {
        return if (descriptor.isBigDecimalDescriptor()) {
            createBigDecimal(descriptor)
        } else if (descriptor.elementsCount > 0) {
            createStruct(descriptor)
        } else {
            BflStructureUnit
        }
    }

    private fun SerialDescriptor.isBigDecimalDescriptor(): Boolean =
        serialName == FixedLengthFloatingPointSerializer.FLOAT ||
            serialName == FixedLengthFloatingPointSerializer.DOUBLE ||
            serialName.startsWith(FixedLengthFloatingPointSerializer.BIG_DECIMAL_PREFIX)

    private fun SerialDescriptor.elements(): Sequence<Pair<String, SerialDescriptor>> =
        (0 until elementsCount).asSequence().map { elementIndex ->
            Pair(
                getElementName(elementIndex),
                getElementDescriptor(elementIndex)
            )
        }

    @Suppress("unchecked")
    private fun createStruct(descriptor: SerialDescriptor): BflStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return BflStructureClass(
            className = fixedLengthSerialDescriptor.serialName,
            familyClassName = tryGetFamilyClassName(fixedLengthSerialDescriptor),
            byteSize = fixedLengthSerialDescriptor.byteSize,
            fields = fixedLengthSerialDescriptor.elements().map { (elementName, elementDescriptor) ->
                BflStructureField(
                    fieldName = elementName,
                    fieldType = generate(elementDescriptor)
                )
            }.toList()
        )
    }

    private fun tryGetFamilyClassName(fixedLengthSerialDescriptor: FixedLengthSerialDescriptor): String? =
        try {
            fixedLengthSerialDescriptor.serialName.tryGetKClass()
                ?.let { klass ->
                    ContractStateVersionFamilyRegistry
                        .familyOf(klass)
                        .familyClass.qualifiedName!!
                }
        } catch (e: Exception) {
            null
        }

    private fun createBigDecimal(descriptor: SerialDescriptor): BflStructureBigDecimal {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        val annotation = descriptor.getAnnotation<BigDecimalSizeAnnotation>()
        return BflStructureBigDecimal(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            kind = fixedLengthSerialDescriptor.serialName,
            integerSize = annotation.integerSize,
            fractionSize = annotation.fractionSize,
        )
    }

    private fun createList(descriptor: SerialDescriptor): BflStructureList {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return BflStructureList(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = descriptor.getAnnotation<SizeAnnotation>().value,
            elementType = generate(
                descriptor // See [FixedLengthCollectionSerializer]
                    .getElementDescriptorByName("values") // `List<T>` descriptor
                    .getElementDescriptorByName("0")
            )
        )
    }

    private fun createArray(descriptor: SerialDescriptor): BflStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return BflStructureArray(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = fixedLengthSerialDescriptor.getAnnotation<SizeAnnotation>().value,
            elementType = generate(
                descriptor // See [ExactLengthCollectionSerializer]
                    .getElementDescriptorByName("values") // `List<T>` descriptor
                    .getElementDescriptorByName("0")
            )
        )
    }

    private fun createString(descriptor: SerialDescriptor, encoding: String): BflStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return BflStructureString(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = fixedLengthSerialDescriptor.getAnnotation<SizeAnnotation>().value,
            encoding = encoding
        )
    }

    private fun createMap(descriptor: SerialDescriptor): BflStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return BflStructureMap(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = fixedLengthSerialDescriptor.getAnnotation<SizeAnnotation>().value,
            keyType = generate(
                descriptor // See [FixedLengthMapSerializer]
                    .getElementDescriptorByName("values") // `MapEntry<K, V>` descriptor
                    .getElementDescriptorByName("0")
                    .getElementDescriptorByName("first") // `K` descriptor
            ),
            valueType = generate(
                descriptor // See [FixedLengthMapSerializer]
                    .getElementDescriptorByName("values") // `MapEntry<K, V>` descriptor
                    .getElementDescriptorByName("0")
                    .getElementDescriptorByName("second") // `V` descriptor
            )
        )
    }
}
