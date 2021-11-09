package com.ing.zkflow.serialization.serializer

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

sealed class FixedLengthFloatingPointSerializer<T : Any> (
    integerPrecision: Int,
    fractionPrecision: Int,
    private val conversion: (T) -> BigDecimal
) : KSerializerWithDefault<T> {

    private val integerSerializer = FixedLengthByteArraySerializer(integerPrecision)
    private val fractionSerializer = FixedLengthByteArraySerializer(fractionPrecision)

    @Suppress("MagicNumber")
    private enum class FloatingKind(val id: Byte) {
        FLOAT(1),
        DOUBLE(2),
        BIG_DECIMAL(3);

        companion object {
            fun <T> forValue(value: T): FloatingKind = when (value) {
                is Float -> FLOAT
                is Double -> DOUBLE
                is BigDecimal -> BIG_DECIMAL
                else -> error("Type ${value?.let{it::class.qualifiedName} ?: value} is not a floating-point type")
            }
            fun forId(id: Byte): FloatingKind = values().single { it.id == id }
        }
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FixedLengthFloatingPoint") {
        element("kind", Byte.serializer().descriptor)
        element("sign", Byte.serializer().descriptor)
        element("integer", integerSerializer.descriptor)
        element("fraction", fractionSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: T) {
        val (sign, integer, fraction) = conversion(value).asByteTriple()
        encoder.run {
            encodeByte(FloatingKind.forValue(value).id)
            encodeByte(sign)
            encodeSerializableValue(integerSerializer, integer)
            encodeSerializableValue(fractionSerializer, fraction)
        }
    }

    override fun deserialize(decoder: Decoder): T = decoder.run {
        val kind = FloatingKind.forId(decodeByte())
        val sign = decodeByte()
        val integer = decodeSerializableValue(integerSerializer)
        val fraction = decodeSerializableValue(fractionSerializer)

        val bigDecimal = toBigDecimal(Triple(sign, integer, fraction))

        @Suppress("UNCHECKED_CAST")
        when (kind) {
            FloatingKind.FLOAT -> bigDecimal.toFloat()
            FloatingKind.DOUBLE -> bigDecimal.toDouble()
            FloatingKind.BIG_DECIMAL -> bigDecimal
        } as T
    }

    private fun BigDecimal.asByteTriple(): Triple<Byte, ByteArray, ByteArray> {
        val (integerPart, fractionalPart) = representOrThrow()
        val sign = signum().toByte()
        val integer = integerPart.toListOfDecimals().reversedArray()
        val fraction = (fractionalPart?.toListOfDecimals() ?: ByteArray(0))
        return Triple(sign, integer, fraction)
    }

    private fun BigDecimal.representOrThrow(): Pair<String, String?> {
        val integerFractionPair = toPlainString().removePrefix("-").split(".")

        val integerPart = integerFractionPair[0]
        val fractionalPart = integerFractionPair.getOrNull(1)

        return Pair(integerPart, fractionalPart)
    }

    private fun String.toListOfDecimals() = map {
        Character.getNumericValue(it).toByte()
    }.toByteArray()

    private fun toBigDecimal(byteTriple: Triple<Byte, ByteArray, ByteArray>): BigDecimal {
        val integer = byteTriple.second.reversedArray().joinToString(separator = "") { "$it" }
        val fraction = byteTriple.third.joinToString(separator = "") { "$it" }
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (byteTriple.first == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit)
    }

    companion object {
        const val DOUBLE_INTEGER_PART_MAX_PRECISION = 309
        const val DOUBLE_FRACTION_PART_MAX_PRECISION = 325

        const val FLOAT_INTEGER_PART_MAX_PRECISION = 39
        const val FLOAT_FRACTION_PART_MAX_PRECISION = 46
    }

    // Variants.
    open class BigDecimalSerializer(integerPrecision: Int, fractionPrecision: Int) :
        FixedLengthFloatingPointSerializer<BigDecimal>(integerPrecision, fractionPrecision, { it }) {
        override val default: BigDecimal = BigDecimal.ZERO
    }

    object DoubleSerializer : FixedLengthFloatingPointSerializer<Double>(
        DOUBLE_INTEGER_PART_MAX_PRECISION, DOUBLE_FRACTION_PART_MAX_PRECISION, { it.toBigDecimal() }
    ) {
        override val default = 0.0
    }

    @Suppress("MagicNumber")
    object FloatSerializer : FixedLengthFloatingPointSerializer<Float>(
        FLOAT_INTEGER_PART_MAX_PRECISION, FLOAT_FRACTION_PART_MAX_PRECISION, { it.toBigDecimal() }
    ) {
        override val default = 0.0.toFloat()
    }
}
