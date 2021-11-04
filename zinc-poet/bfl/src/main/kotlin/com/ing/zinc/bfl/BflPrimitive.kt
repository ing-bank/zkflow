package com.ing.zinc.bfl

import com.ing.zinc.bfl.BflType.Companion.BITS_PER_BYTE
import java.util.Locale

@Suppress("MagicNumber")
enum class BflPrimitive(
    override val id: String,
    override val bitSize: Int,
) : BflType {
    U8("u8", 1 * BITS_PER_BYTE),
    U16("u16", 2 * BITS_PER_BYTE),
    U24("u24", 3 * BITS_PER_BYTE),
    U32("u32", 4 * BITS_PER_BYTE),
    U64("u64", 8 * BITS_PER_BYTE),
    U128("u128", 16 * BITS_PER_BYTE),
    I8("i8", 1 * BITS_PER_BYTE),
    I16("i16", 2 * BITS_PER_BYTE),
    I32("i32", 4 * BITS_PER_BYTE),
    I64("i64", 8 * BITS_PER_BYTE),
    I128("i128", 16 * BITS_PER_BYTE),
    Bool("bool", 1);

    override fun typeName() = id.capitalize(Locale.getDefault())

    override fun deserializeExpr(options: DeserializationOptions): String {
        return when (this) {
            Bool -> "${options.bitArrayVariable}[${options.offset}]"
            U8, U16, U24, U32, U64, U128 -> {
                generateParseIntegerFromBits(options, false)
            }
            I8, I16, I32, I64, I128 -> {
                generateParseIntegerFromBits(options, true)
            }
        }
    }

    private fun generateParseIntegerFromBits(
        options: DeserializationOptions,
        isSigned: Boolean
    ): String {
        val bSize = sizeExpr()
        val bits = options.generateVariable("bits")
        val i = options.generateVariable("i")
        return """
            {
                let mut $bits: [bool; $bSize] = [false; $bSize];
                for $i in (0 as u24)..$bSize {
                    $bits[$i] = ${options.bitArrayVariable}[$i + ${options.offset}];
                }
                std::convert::${if (isSigned) "from_bits_signed" else "from_bits_unsigned"}($bits)
            }
        """.trimIndent()
    }

    override fun defaultExpr(): String {
        return when (this) {
            Bool -> "false"
            U8 -> "0"
            else -> "0 as $id"
        }
    }

    override fun equalsExpr(self: String, other: String): String {
        return "$self == $other"
    }

    override fun accept(visitor: TypeVisitor) {
        // NOOP
    }

    companion object {
        fun isPrimiviteIdentifier(identifier: String): Boolean {
            return values().find { it.id == identifier } != null
        }
    }
}
