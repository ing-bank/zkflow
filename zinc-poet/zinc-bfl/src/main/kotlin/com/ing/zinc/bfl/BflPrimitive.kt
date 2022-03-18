package com.ing.zinc.bfl

import com.ing.zinc.bfl.generator.TransactionComponentOptions
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.Tree
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
    Bool("bool", 8);

    override fun typeName() = id.capitalize(Locale.getDefault())

    override fun deserializeExpr(
        transactionComponentOptions: TransactionComponentOptions,
        offset: String,
        variablePrefix: String,
        witnessVariable: String
    ): String {
        return when (this) {
            Bool -> "$witnessVariable[$offset + 7 as u24]"
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
        val i = "${variablePrefix}_i"
        val bits = "${variablePrefix}_bits"
        val o = "${variablePrefix}_offset"
        return """
            {
                let $o: u24 = $offset;
                let mut $bits: [bool; $bSize] = [false; $bSize];
                for $i in (0 as u24)..$bSize {
                    $bits[$i] = $witnessVariable[$i + $o];
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

    override fun toZincType(): ZincPrimitive = when (this) {
        U8 -> ZincPrimitive.U8
        U16 -> ZincPrimitive.U16
        U24 -> ZincPrimitive.U24
        U32 -> ZincPrimitive.U32
        U64 -> ZincPrimitive.U64
        U128 -> ZincPrimitive.U128
        I8 -> ZincPrimitive.I8
        I16 -> ZincPrimitive.I16
        I32 -> ZincPrimitive.I32
        I64 -> ZincPrimitive.I64
        I128 -> ZincPrimitive.I128
        Bool -> ZincPrimitive.Bool
    }

    override fun toStructureTree(): Tree<BflSized, BflSized> {
        return Tree.leaf(toNodeDescriptor())
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
