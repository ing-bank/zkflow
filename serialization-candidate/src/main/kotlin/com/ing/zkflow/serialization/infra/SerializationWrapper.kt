package com.ing.zkflow.serialization.infra

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.serializer.ByteSerializer
import com.ing.zkflow.serialization.serializer.FixedLengthListSerializer
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SerializationWrapper<M : Any, S : List<Byte>>(
    val metadata: M,
    val serialization: S
)

/**
 * For a given sequence of bytes, this function derives a fixed length serializer fitting the bytes exactly.
 */
fun ByteArray.exactLengthSerializer() =
    FixedLengthListSerializer(size, ByteSerializer)

/**
 * Wrap serialization with metadata and serialize it into a single byte array.
 */
fun <M : Any> ByteArray.wrapSerialization(scheme: BinaryFixedLengthScheme, metadata: M, metadataSerializer: KSerializer<M>): ByteArray {
    val wrappedSerialization = SerializationWrapper(metadata, serialization = toList())

    return scheme.encodeToBinary(
        SerializationWrapper.serializer(metadataSerializer, exactLengthSerializer()),
        wrappedSerialization
    )
}

/**
 * Takes serialized data and attempts to deserialize it into a SerializationWrapper.
 */
fun <M : Any> ByteArray.unwrapSerialization(scheme: BinaryFixedLengthScheme, metadataSerializer: KSerializer<M>): SerializationWrapper<M, List<Byte>> {
    val metadataByteSize = metadataSerializer
        .descriptor
        .toFixedLengthSerialDescriptorOrThrow()
        .byteSize

    return scheme.decodeFromBinary(
        SerializationWrapper.serializer(
            metadataSerializer,
            exactLengthListByteDeserializer(size - metadataByteSize)
        ),
        this
    )
}

/**
 * For a given sequence of bytes, this function derives a fixed length serializer which
 * will attempt exhausting the full sequence.
 */
private fun exactLengthListByteDeserializer(serializationLength: Int) =
    FixedLengthListSerializer(serializationLength - Int.SIZE_BYTES, ByteSerializer)
