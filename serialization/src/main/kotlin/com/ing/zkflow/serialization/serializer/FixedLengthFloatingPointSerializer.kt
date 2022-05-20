package com.ing.zkflow.serialization.serializer

import com.ing.zkflow.serialization.FixedLengthKSerializerWithDefault
import com.ing.zkflow.serialization.toFixedLengthSerialDescriptorOrThrow
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

/**
 * [FixedLengthFloatingPointSerializer] is used to serialize [Float], [Double] and [BigDecimal] types.
 * The serialized structure is modelled after the [BigDecimal] java type.
 *
 * The encoded representation of a [BigDecimal] is a byte stream with the next elements in the following order:
 *
 * - Byte: kind, 1 (Float), 2 (Double) or 3 (BigDecimal)
 * - Byte: sign, -1 (Negative), 0 (Zero), or 1 (Positive)
 * - ByteArray for integer part, fixed size
 *   - Int: number of significant decimals in integer part
 *   - Byte[]: decimals of the integer part with little endian encoding
 * - ByteArray for fraction part, fixed size
 *   - Int: number of significant decimals in fraction part
 *   - Byte[]: decimals of the fraction part with big endian encoding
 *
 * Note that the integer and fraction part are encoded with different endianness, this will be
 * explained further in the section about [encoding](#encoding-and-endianness), but in short the following
 *
 * - integer: little-endian encoded ByteArray, so the number 123 is encoded as ByteArray(3): [3, 2, 1]
 * - fraction: big-endian encoded ByteArray, so the fraction of 0.123 is encoded as ByteArray(3): [1, 2, 3]
 *
 * # Example
 *
 * The size of the encoded [BigDecimal] is retrieved from the `@BigDecimalSize` annotation. So consider the following
 * definition:
 *
 * ```kotlin
 * @ZKP
 * data class Data(
 *     val value: @BigDecimalSize([6, 4]) BigDecimal
 * )
 * ```
 *
 * Then the number "123.456" is encoded as:
 *
 * ```
 * [3, 1, 0, 0, 0, 3, 3, 2, 1, 0, 0, 0, 0, 0, 0, 3, 4, 5, 6, 0]
 * │ ││ ││ ByteArray                  ││ ByteArray            │
 * └┬┘└┬┘├──────────┬┬────────────────┤├──────────┬┬──────────┤
 *  │  │ │ size     ││ values         ││ size     ││ values   │
 *  │  │ └────┬─────┘└──────┬─────────┘└────┬─────┘└────┬─────┘
 *  │  │      │             │               │           └─ fraction: "456"
 *  │  │      │             │               └─ fraction length: 3
 *  │  │      │             └─ integer: "123", note the little-endian encoding
 *  │  │      └─ integer length: 3
 *  │  └─ 1, positive number
 *  └─ 3, BIG_DECIMAL kind
 * ```
 *
 * # Encoding and Endianness
 *
 * The difference in endianness is explained by the fact that some processors implemented in exotic languages, e.g.
 * [Zinc][1], may not be able to use the size bytes when deserializing the bytes. Additionally, the whole byte array is
 * now a valid representation of the part in the given endianness. This means that processors can either work with the
 * first `n` bytes, or just process the whole array of bytes independently of the length field.
 *
 * Given the value in the [example](#example), processing the whole integer part with little-endianness will result in:
 * "000123", which equals "123".
 *
 * And similarly for the fraction, processing the whole fraction part with big-endianness will result in ".4560", which
 * equals ".456".
 *
 * [1]: https://zinc.zksync.io/ "Zinc"
 */
sealed class FixedLengthFloatingPointSerializer<T : Any> (
    integerPrecision: Int,
    fractionPrecision: Int,
    kind: FloatingKind,
    private val conversion: (T) -> BigDecimal
) : FixedLengthKSerializerWithDefault<T> {

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

    private val serialName = when (kind) {
        FloatingKind.FLOAT -> "Float"
        FloatingKind.DOUBLE -> "Double"
        FloatingKind.BIG_DECIMAL -> "BigDecimal_${integerPrecision}_$fractionPrecision"
    }

    override val descriptor =
        buildClassSerialDescriptor(serialName) {
            element("kind", ByteSerializer.descriptor)
            element("sign", ByteSerializer.descriptor)
            element("integer", integerSerializer.descriptor)
            element("fraction", fractionSerializer.descriptor)
            annotations = listOf(
                BigDecimalSizeAnnotation(integerPrecision, fractionPrecision)
            )
        }.toFixedLengthSerialDescriptorOrThrow()

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
        FixedLengthFloatingPointSerializer<BigDecimal>(integerPrecision, fractionPrecision, FloatingKind.BIG_DECIMAL, { it }) {
        override val default: BigDecimal = BigDecimal.ZERO
    }

    object DoubleSerializer : FixedLengthFloatingPointSerializer<Double>(
        DOUBLE_INTEGER_PART_MAX_PRECISION, DOUBLE_FRACTION_PART_MAX_PRECISION, FloatingKind.DOUBLE, { it.toBigDecimal() }
    ) {
        override val default = 0.0
    }

    @Suppress("MagicNumber")
    object FloatSerializer : FixedLengthFloatingPointSerializer<Float>(
        FLOAT_INTEGER_PART_MAX_PRECISION, FLOAT_FRACTION_PART_MAX_PRECISION, FloatingKind.FLOAT, { it.toBigDecimal() }
    ) {
        override val default = 0.0.toFloat()
    }
}

data class BigDecimalSizeAnnotation(val integerSize: Int, val fractionSize: Int) : Annotation
