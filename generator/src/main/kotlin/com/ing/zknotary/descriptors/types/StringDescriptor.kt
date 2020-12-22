package com.ing.zknotary.descriptors.types

import com.ing.zknotary.annotations.SizedString
import com.ing.zknotary.descriptors.TypeDescriptor
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName

class StringDescriptor(private val length: Int, private val filler: Char) : TypeDescriptor(SizedString::class, listOf()) {
    override val isTransient = false

    override val type: TypeName
        get() = definition

    override val default: CodeBlock
        get() = CodeBlock.of("SizedString( %L, '%L' )", length, filler)

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val map = mutableMapOf(
            "propertyName" to propertyName,
            "length" to length,
            "default" to "'$filler'"
        )

        return CodeBlock.builder()
            .addNamed(
                "SizedString(" +
                    "\n⇥n = %length:L," +
                    "\nstring = %propertyName:L," +
                    "\ndefault = %default:L\n⇤)",
                map
            ).build()
    }
}
