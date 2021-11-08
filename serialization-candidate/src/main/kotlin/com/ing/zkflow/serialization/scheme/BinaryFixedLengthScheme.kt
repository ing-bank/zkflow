package com.ing.zkflow.serialization.scheme

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

interface BinaryFixedLengthScheme {
    fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray
    fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T
}

/**
 * Scheme serializing a value into a fixed length sequence of bits.
 */
object BitBinaryFixedLengthScheme : BinaryFixedLengthScheme {
    override fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { stream ->
                BitBinaryEncoder(stream).encodeSerializableValue(serializer, value)
            }
            output.toByteArray()
        }

    override fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T =
        ByteArrayInputStream(data).use { input ->
            DataInputStream(input).use { stream ->
                BitBinaryDecoder(stream).decodeSerializableValue(deserializer)
            }
        }
}

/**
 * Scheme serializing a value into a fixed length sequence of bytes.
 */
object ByteBinaryFixedLengthScheme : BinaryFixedLengthScheme {
    override fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { stream ->
                ByteBinaryEncoder(stream).encodeSerializableValue(serializer, value)
            }
            output.toByteArray()
        }

    override fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T =
        ByteArrayInputStream(data).use { input ->
            DataInputStream(input).use { stream ->
                ByteBinaryDecoder(stream).decodeSerializableValue(deserializer)
            }
        }
}
