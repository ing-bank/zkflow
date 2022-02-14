package com.ing.zkflow.serialization.engine

import com.ing.zkflow.serialization.scheme.BinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.BitBinaryFixedLengthScheme
import com.ing.zkflow.serialization.scheme.ByteBinaryFixedLengthScheme
import kotlinx.serialization.KSerializer

public open class BFLEngine(private val scheme: BinaryFixedLengthScheme) : SerdeEngine {
    /**
     * A scale factor `s` expected to solve the equation
     * `s` * `expected size in bytes` = `size of the serialization in bytes\bits`
     */
    public val bytesScaler: Int = when (scheme.unit) {
        BinaryFixedLengthScheme.Unit.BITS -> 8
        BinaryFixedLengthScheme.Unit.BYTES -> 1
    }

    override fun <T> serialize(strategy: KSerializer<T>, value: T, shouldPrint: Boolean): ByteArray {
        if (shouldPrint) { println("Serializing:\n$value") }
        return scheme.encodeToBinary(strategy, value).also {
            if (shouldPrint) { printData(it, "Serialized") }
        }
    }

    override fun <T> deserialize(strategy: KSerializer<T>, data: ByteArray, shouldPrint: Boolean): T {
        if (shouldPrint) { printData(data, "Deserializing") }
        return scheme.decodeFromBinary(strategy, data).also {
            if (shouldPrint) println("Deserialized:\n$it")
        }
    }

    private fun printData(data: ByteArray, title: String) {
        val windowedAsBytes = data.asSequence().windowed(8, 8, true)
        val windowedString = windowedAsBytes.joinToString(", ") { it.joinToString(",") }
        println("$title (${scheme.unit}):\n$windowedString")
    }

    public object Bits : BFLEngine(BitBinaryFixedLengthScheme)
    public object Bytes : BFLEngine(ByteBinaryFixedLengthScheme)
}
