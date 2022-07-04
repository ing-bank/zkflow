package com.ing.zkflow.zinc.poet.generate.structure

import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.versioning.ContractStateVersionFamilyRegistry
import com.ing.zkflow.serialization.FixedLengthSerialDescriptor
import com.ing.zkflow.serialization.FixedLengthType
import com.ing.zkflow.serialization.serializer.BigDecimalSizeAnnotation
import com.ing.zkflow.serialization.serializer.FixedLengthFloatingPointSerializer
import com.ing.zkflow.serialization.serializer.SizeAnnotation
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import com.ing.zkflow.util.requireNotNull
import com.ing.zkflow.util.tryGetKClass
import com.ing.zkflow.zinc.poet.generate.getAnnotation
import com.ing.zkflow.zinc.poet.generate.getElementDescriptorByName
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import net.corda.core.contracts.ContractState

object ZkpStructureGenerator {
    fun generate(descriptor: SerialDescriptor): ZkpStructureType {
        /**
         * We handle nullability as soon as possible, so that we can use our special [option]
         */
        return if (descriptor.isNullable) {
            val innerType = generate(
                descriptor // see [NullableSerializer]
                    .getElementDescriptorByName("value") // `T` descriptor
            )
            ZkpStructureNullable(
                byteSize = innerType.byteSize + 1,
                innerType = innerType,
            )
        } else {
            generateBflType(descriptor)
        }
    }

    @Suppress("ComplexMethod")
    private fun generateBflType(descriptor: SerialDescriptor): ZkpStructureType {
        return when (FixedLengthType.tryFromSerialName(descriptor.serialName)) {
            FixedLengthType.SET,
            FixedLengthType.LIST -> createList(descriptor)
            FixedLengthType.MAP -> createMap(descriptor)
            FixedLengthType.BYTE_ARRAY -> createArray(descriptor)
            FixedLengthType.ASCII_STRING -> createString(descriptor, "Ascii")
            FixedLengthType.UTF8_STRING -> createString(descriptor, "Utf8")
            FixedLengthType.UTF16_STRING -> createString(descriptor, "Utf16")
            FixedLengthType.UTF32_STRING -> createString(descriptor, "Utf32")
            FixedLengthType.BOOLEAN -> ZkpStructurePrimitive(Boolean::class.qualifiedName!!, Byte.SIZE_BYTES)
            FixedLengthType.BYTE -> ZkpStructurePrimitive(Byte::class.qualifiedName!!, Byte.SIZE_BYTES)
            FixedLengthType.UBYTE -> ZkpStructurePrimitive(UByte::class.qualifiedName!!, UByte.SIZE_BYTES)
            FixedLengthType.SHORT -> ZkpStructurePrimitive(Short::class.qualifiedName!!, Short.SIZE_BYTES)
            FixedLengthType.USHORT -> ZkpStructurePrimitive(UShort::class.qualifiedName!!, UShort.SIZE_BYTES)
            FixedLengthType.INT -> ZkpStructurePrimitive(Int::class.qualifiedName!!, Int.SIZE_BYTES)
            FixedLengthType.UINT -> ZkpStructurePrimitive(UInt::class.qualifiedName!!, UInt.SIZE_BYTES)
            FixedLengthType.LONG -> ZkpStructurePrimitive(Long::class.qualifiedName!!, Long.SIZE_BYTES)
            FixedLengthType.ULONG -> ZkpStructurePrimitive(ULong::class.qualifiedName!!, ULong.SIZE_BYTES)
            FixedLengthType.EXACT_LIST -> createArray(descriptor)
            null -> when (descriptor.kind) {
                SerialKind.ENUM -> createEnum(descriptor)
                StructureKind.CLASS -> createClass(descriptor)
                else -> error("No handler found for ${descriptor.kind}: ${descriptor.serialName}.")
            }
        }
    }

    private fun createEnum(serialDescriptor: SerialDescriptor): ZkpStructureType =
        ZkpStructureEnum(serialDescriptor.serialName)

    private fun createClass(descriptor: SerialDescriptor): ZkpStructureType {
        return if (descriptor.isBigDecimalDescriptor()) {
            createBigDecimal(descriptor)
        } else {
            createStruct(descriptor)
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
    private fun createStruct(descriptor: SerialDescriptor): ZkpStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return ZkpStructureClass(
            serialName = fixedLengthSerialDescriptor.serialName,
            familyClassName = tryGetFamilyClassName(fixedLengthSerialDescriptor),
            serializationId = tryGetSerializationId(fixedLengthSerialDescriptor),
            byteSize = fixedLengthSerialDescriptor.byteSize,
            fields = fixedLengthSerialDescriptor.elements().map { (elementName, elementDescriptor) ->
                ZkpStructureField(
                    fieldName = elementName,
                    fieldType = generate(elementDescriptor)
                )
            }.toList()
        )
    }

    private fun tryGetFamilyClassName(fixedLengthSerialDescriptor: FixedLengthSerialDescriptor): String? =
        try {
            fixedLengthSerialDescriptor.serialName.tryGetKClass<ContractState>()
                ?.let { klass ->
                    ContractStateVersionFamilyRegistry
                        .familyOf(klass)
                        .requireNotNull { "Could not find Version family for $klass." }
                        .familyClass
                        .qualifiedName
                }
        } catch (e: Exception) {
            null
        }

    private fun tryGetSerializationId(fixedLengthSerialDescriptor: FixedLengthSerialDescriptor): Int? =
        try {
            fixedLengthSerialDescriptor.serialName.tryGetKClass<ContractState>()
                ?.let { klass ->
                    ContractStateSerializerRegistry.identify(klass)
                }
        } catch (e: Exception) {
            null
        }

    private fun createBigDecimal(descriptor: SerialDescriptor): ZkpStructureBigDecimal {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        val annotation = descriptor.getAnnotation<BigDecimalSizeAnnotation>()
        return ZkpStructureBigDecimal(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            serialName = fixedLengthSerialDescriptor.serialName,
            integerSize = annotation.integerSize,
            fractionSize = annotation.fractionSize,
        )
    }

    private fun createList(descriptor: SerialDescriptor): ZkpStructureList {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return ZkpStructureList(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = descriptor.getAnnotation<SizeAnnotation>().value,
            elementType = generate(
                descriptor // See [FixedLengthCollectionSerializer]
                    .getElementDescriptorByName("values") // `List<T>` descriptor
                    .getElementDescriptorByName("0")
            )
        )
    }

    private fun createArray(descriptor: SerialDescriptor): ZkpStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return ZkpStructureArray(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = fixedLengthSerialDescriptor.getAnnotation<SizeAnnotation>().value,
            elementType = generate(
                descriptor // See [ExactLengthCollectionSerializer]
                    .getElementDescriptorByName("values") // `List<T>` descriptor
                    .getElementDescriptorByName("0")
            )
        )
    }

    private fun createString(descriptor: SerialDescriptor, encoding: String): ZkpStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return ZkpStructureString(
            byteSize = fixedLengthSerialDescriptor.byteSize,
            capacity = fixedLengthSerialDescriptor.getAnnotation<SizeAnnotation>().value,
            encoding = encoding
        )
    }

    private fun createMap(descriptor: SerialDescriptor): ZkpStructureType {
        val fixedLengthSerialDescriptor: FixedLengthSerialDescriptor = descriptor.toFixedLengthSerialDescriptorOrThrow()
        return ZkpStructureMap(
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
