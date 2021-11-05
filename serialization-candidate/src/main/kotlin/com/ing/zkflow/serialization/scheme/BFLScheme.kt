package com.ing.zkflow.serialization.scheme

import com.ing.zkflow.serialization.utils.binary.Representation
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

open class BFLScheme internal constructor(private val representation: Representation) {
    fun <T> encodeToBinary(serializer: SerializationStrategy<T>, value: T): ByteArray =
        ByteArrayOutputStream().use { output ->
            DataOutputStream(output).use { stream ->
                BinaryEncoder(stream, representation).encodeSerializableValue(serializer, value)
            }
            output.toByteArray()
        }

    fun <T> decodeFromBinary(deserializer: DeserializationStrategy<T>, data: ByteArray): T =
        ByteArrayInputStream(data).use { input ->
            DataInputStream(input).use { stream ->
                BinaryDecoder(stream, representation).decodeSerializableValue(deserializer)
            }
        }
}

object BFLSchemeBits : BFLScheme(Representation.BITS)
object BFLSchemeBytes : BFLScheme(Representation.BYTES)
