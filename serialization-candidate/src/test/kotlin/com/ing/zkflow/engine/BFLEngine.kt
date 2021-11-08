package com.ing.zkflow.engine

import com.ing.zkflow.serialization.utils.binary.Representation
import kotlinx.serialization.KSerializer

open class BFLEngine(representation: Representation) : SerdeEngine {
    private val engine = representation.serializationScheme

    override fun <T> serialize(strategy: KSerializer<T>, value: T, shouldPrint: Boolean): ByteArray {
        if (shouldPrint) { println("Serializing:\n$value") }
        return engine.encodeToBinary(strategy, value).also {
            if (shouldPrint) println("Serialized:\n${it.joinToString(separator = ",")}")
        }
    }

    override fun <T> deserialize(strategy: KSerializer<T>, data: ByteArray, shouldPrint: Boolean): T {
        if (shouldPrint) { println("Deserializing:\n${data.joinToString(separator = ",")}") }

        return engine.decodeFromBinary(strategy, data).also {
            if (shouldPrint) println("Deserialized:\n$it")
        }
    }

    object Bits : BFLEngine(Representation.BITS)
    object Bytes : BFLEngine(Representation.BYTES)
}
