package com.ing.zkflow.serialization.scheme

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

interface BinaryFixedLengthScheme {
    val unit: Unit
    fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray
    fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T

    enum class Unit {
        BITS,
        BYTES
    }
}

/**
 * Scheme serializing a value into a fixed length sequence of bits.
 */
object BitBinaryFixedLengthScheme : BinaryFixedLengthScheme {
    override val unit = BinaryFixedLengthScheme.Unit.BITS

    override fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { stream ->
                BinaryEncoder.BitBinaryEncoder(stream).encodeSerializableValue(serializer, value)
            }
            output.toByteArray()
        }

    override fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T =
        ByteArrayInputStream(data).use { input ->
            DataInputStream(input).use { stream ->
                BinaryDecoder.BitBinaryDecoder(stream).decodeSerializableValue(deserializer)
            }
        }
}

/**
 * Scheme serializing a value into a fixed length sequence of bytes.
 */
object ByteBinaryFixedLengthScheme : BinaryFixedLengthScheme {
    override val unit = BinaryFixedLengthScheme.Unit.BYTES

    override fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { stream ->
                BinaryEncoder.ByteBinaryEncoder(stream).encodeSerializableValue(serializer, value)
            }
            output.toByteArray()
        }

    override fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T =
        ByteArrayInputStream(data).use { input ->
            DataInputStream(input).use { stream ->
                BinaryDecoder.ByteBinaryDecoder(stream).decodeSerializableValue(deserializer)
            }
        }
}
