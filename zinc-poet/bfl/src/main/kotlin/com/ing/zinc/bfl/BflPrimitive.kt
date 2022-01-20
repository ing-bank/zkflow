package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.WitnessGroupOptions
import java.util.Locale

@Suppress("MagicNumber")
enum class BflPrimitive(
    override val id: String,
    override val bitSize: Int,
) : BflType {
    U8("u8", 8),
    U16("u16", 16),
    U24("u24", 24),
    U32("u32", 32),
    U64("u64", 64),
    U128("u128", 128),
    I8("i8", 8),
    I16("i16", 16),
    I32("i32", 32),
    I64("i64", 64),
    I128("i128", 128),
    Bool("bool", 1);

    override fun typeName() = id.capitalize(Locale.getDefault())

    override fun deserializeExpr(
        witnessGroupOptions: WitnessGroupOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        return when (this) {
            Bool -> "$witnessVariable[$offset]"
            U8, U16, U24, U32, U64, U128 -> {
                generateParseIntegerFromBits(offset, variablePrefix, false, witnessVariable)
            }
            I8, I16, I32, I64, I128 -> {
                generateParseIntegerFromBits(offset, variablePrefix, true, witnessVariable)
            }
        }
    }

    private fun generateParseIntegerFromBits(
        offset: String,
        variablePrefix: String,
        isSigned: Boolean,
        witnessVariable: String
    ): String {
        val bSize = sizeExpr()
        val bits = "${variablePrefix}_bits"
        val i = "${variablePrefix}_i"
        return """
            {
                let mut $bits: [bool; $bSize] = [false; $bSize];
                for $i in (0 as u24)..$bSize {
                    $bits[$i] = $witnessVariable[$i + $offset];
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
        private fun tryFromIdentifier(identifier: String): BflPrimitive? {
            return values().find { it.id == identifier }
        }

        fun fromIdentifier(identifier: String): BflPrimitive {
            return tryFromIdentifier(identifier) ?: throw IllegalArgumentException("Not a primitive type: `$identifier`")
        }

        fun isPrimitiveIdentifier(identifier: String): Boolean {
            return tryFromIdentifier(identifier) != null
        }
    }
}