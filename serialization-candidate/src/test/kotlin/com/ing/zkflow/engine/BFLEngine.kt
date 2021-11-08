package com.ing.zkflow.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.BitBinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import kotlinx.serialization.KSerializer

open class BFLEngine(private val scheme: BinaryFixedLengthScheme) : SerdeEngine {

    override fun <T> serialize(strategy: KSerializer<T>, value: T, shouldPrint: Boolean): ByteArray {
        if (shouldPrint) { println("Serializing:\n$value") }
        return scheme.encodeToBinary(strategy, value).also {
            if (shouldPrint) println("Serialized:\n${it.joinToString(separator = ",")}")
        }
    }

    override fun <T> deserialize(strategy: KSerializer<T>, data: ByteArray, shouldPrint: Boolean): T {
        if (shouldPrint) { println("Deserializing:\n${data.joinToString(separator = ",")}") }

        return scheme.decodeFromBinary(strategy, data).also {
            if (shouldPrint) println("Deserialized:\n$it")
        }
    }

    object Bits : BFLEngine(BitBinaryFixedLengthScheme)
    object Bytes : BFLEngine(ByteBinaryFixedLengthScheme)
}
