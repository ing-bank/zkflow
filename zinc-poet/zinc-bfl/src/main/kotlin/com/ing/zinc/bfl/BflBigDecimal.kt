package com.ing.zinc.bfl

import com.ing.zinc.bfl.dsl.ListBuilder.Companion.list
import com.ing.zinc.bfl.generator.CodeGenerationOptions
import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincInvocable
import com.ing.zinc.poet.ZincMethod.Companion.zincMethod
import com.ing.zinc.poet.ZincPrimitive
import com.ing.zinc.poet.indent
import java.util.Objects

class BflBigDecimal(
    val integerSize: Int,
    val fractionSize: Int,
    val overrideName: String? = null
) : BflStruct(
    overrideName ?: "BigDecimal_${integerSize}_$fractionSize",
    listOf(
        Field("kind", BflPrimitive.I8),
        Field("sign", BflPrimitive.I8),
        Field(
            "integer",
            getIntegerFieldType(integerSize)
        ),
        Field(
            "fraction",
            getFractionFieldType(fractionSize)
        )
    )
) {
    companion object {
        fun getIntegerFieldType(integerSize: Int) = list {
            capacity = integerSize
            elementType = BflPrimitive.U8
        }

        fun getFractionFieldType(fractionSize: Int) = list {
            capacity = fractionSize
            elementType = BflPrimitive.U8
        }
    }

    override fun equals(other: Any?): Boolean = when (other) {
        is BflBigDecimal -> integerSize == other.integerSize && fractionSize == other.fractionSize && overrideName == other.overrideName
        else -> false
    }

    override fun hashCode(): Int = Objects.hash(integerSize, fractionSize, overrideName)

    private fun reversedIndex(sizeExpression: String) = "$sizeExpression as u32 - i - 1 as u32"

    private fun comparePart(sizeExpression: String, partName: String, isLittleEndian: Boolean) = """
        for i in (0 as u32)..$sizeExpression {
            let index = ${if (isLittleEndian) reversedIndex(sizeExpression) else "i"};
            if result == 0 as i8 {
                result = if self.$partName.values[index] > other.$partName.values[index] {
                    1 as i8
                } else {
                    if self.$partName.values[index] < other.$partName.values[index] {
                        -1
                    } else {
                        0 as i8
                    }
                };
            }
        }
    """.trimIndent()

    private fun sumPart(sizeExpression: String, partName: String, isLittleEndian: Boolean) = """
        let mut $partName: [u8; $sizeExpression] = [0; $sizeExpression];
        for i in (0 as u32)..$sizeExpression {
            let index = ${if (isLittleEndian) "i" else reversedIndex(sizeExpression)};
            let value =
                self.$partName.values[index] + other.$partName.values[index] + carry;
            if value >= 10 {
                carry = 1;
                $partName[index] = value - 10;
            } else {
                carry = 0;
                $partName[index] = value;
            }
        }
    """.trimIndent()

    private fun subtractPart(sizeExpression: String, partName: String, isLittleEndian: Boolean) = """
        let mut $partName: [u8; $sizeExpression] = [0; $sizeExpression];
        for i in (0 as u32)..$sizeExpression {
            let index = ${if (isLittleEndian) "i" else reversedIndex(sizeExpression)};
            let value = self.$partName.values[index] as i8 - other.$partName.values[index] as i8 - carry as i8;
            if value < 0 as i8 {
                carry = 1;
                $partName[index] = (10 as i8 + value) as u8;
            } else {
                carry = 0;
                $partName[index] = value as u8;
            }
        }        
    """.trimIndent()

    @Suppress("LongMethod")
    override fun generateMethods(codeGenerationOptions: CodeGenerationOptions): List<ZincInvocable> {
        return super.generateMethods(codeGenerationOptions) + listOf(
            zincMethod {
                name = "_compare_magnitude"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = ZincPrimitive.I8
                body = """
                    let mut result: i8 = 0 as i8;
                    ${comparePart("$integerSize", "integer", true).indent(20.spaces)}
                    
                    if result == 0 as i8 {
                        ${comparePart("$fractionSize", "fraction", false).indent(24.spaces)}
                        result
                    } else {
                        result
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "compare"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = ZincPrimitive.I8
                body = """
                    if self.sign == other.sign {
                        if self.sign == 0 as i8 {
                            0 as i8
                        } else {
                            self.sign * self._compare_magnitude(other)
                        }
                    } else {
                        if self.sign > other.sign {
                            1 as i8
                        } else {
                            -1
                        }
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "_sum_magnitude"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    let mut carry = 0;
                    ${sumPart("$fractionSize", "fraction", false).indent(20.spaces)}
                    ${sumPart("$integerSize", "integer", true).indent(20.spaces)}
                    assert!(carry != 1, "Magnitude exceeds the maximum stored value");
                    Self {
                        kind: self.kind,
                        sign: self.sign,
                        integer: ${getIntegerFieldType(integerSize).id}::list_of(integer),
                        fraction: ${getFractionFieldType(fractionSize).id}::list_of(fraction),
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "_subtract_magnitude"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    let mut carry = 0;
                    ${subtractPart("$fractionSize", "fraction", false).indent(20.spaces)}
                    ${subtractPart("$integerSize", "integer", true).indent(20.spaces)}
                    let sign = if carry == 1 { -1 } else { 1 as i8 };
                    Self {
                        kind: self.kind,
                        sign: self.sign,
                        integer: ${getIntegerFieldType(integerSize).id}::list_of(integer),
                        fraction: ${getFractionFieldType(fractionSize).id}::list_of(fraction),
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "_handle_different_signs"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    let comparison = self._compare_magnitude(other);
                    if comparison == 0 as i8 {
                        empty()
                    } else {
                        let absolute_diff = if comparison == 1 as i8 {
                            self._subtract_magnitude(other)
                        } else {
                            other._subtract_magnitude(self)
                        };
                        Self {
                            kind: self.kind,
                            sign: comparison * self.sign,
                            integer: absolute_diff.integer,
                            fraction: absolute_diff.fraction,
                        }
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "_handle_same_sign"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    if other.sign == 0 as i8 {
                        empty()
                    } else {
                        self._sum_magnitude(other)
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "plus"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    if self.sign == 0 as i8 {
                        other
                    } else {
                        if other.sign == 0 as i8 {
                            self
                        } else {
                            if self.sign == other.sign {
                                self._handle_same_sign(other)
                            } else {
                                self._handle_different_signs(other)
                            }
                        }
                    }
                """.trimIndent()
            },
            zincMethod {
                name = "minus"
                parameter {
                    name = "other"
                    type = this@BflBigDecimal.toZincType()
                }
                returnType = this@BflBigDecimal.toZincType()
                body = """
                    let negated = Self {
                        kind: other.kind,
                        sign: -other.sign,
                        integer: other.integer,
                        fraction: other.fraction,
                    };
	            
                    plus(self, negated)
                """.trimIndent()
            }
        )
    }
}
