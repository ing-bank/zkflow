package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.SizedList
import com.ing.zknotary.descriptors.UserTypeDescriptor
import com.ing.zknotary.descriptors.TypeDescriptor
import com.ing.zknotary.descriptors.describe
import com.ing.zknotary.util.expectAnnotation
import com.ing.zknotary.util.expectArgument
import com.ing.zknotary.util.getArgumentOrDefault
import com.squareup.kotlinpoet.CodeBlock

class ListDescriptor private constructor(private val size: Int, innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(
    SizedList::class,
    innerDescriptors
) {
    companion object {
        fun fromKSP(list: KSType, userTypeDescriptor: UserTypeDescriptor): ListDescriptor {
            // List must be annotated with Sized.
            val sized = list.expectAnnotation<Sized>()

            // TODO Too many hardcoded things: property names and their types.
            val size = sized.expectArgument<Int>("size")
            val useDefault = sized.getArgumentOrDefault("useDefault", false)

            // List must have an inner type.
            val listType = list.arguments.single().type?.resolve()
                ?: error("List must have a type")

            val innerSupport = if (useDefault) {
                UserTypeDescriptor.Default
            } else {
                userTypeDescriptor
            }

            return ListDescriptor(size, listOf(listType.describe(innerSupport)))
        }
    }

    override val default: CodeBlock
        get() {
            val innerType = innerDescriptors.getOrNull(0)
                ?: error("SizedList must declare type of its elements")

            return CodeBlock.of(
                "SizedList( %L, %L )",
                size, innerType.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val listType = innerDescriptors.getOrNull(0)
            ?: error("SizedList must declare type of its elements")

        var map = mapOf(
            "propertyName" to propertyName,
            "size" to size,
            "default" to listType.default
        )
        var mapper = ""

        if (listType.isTransient) {
            val itName = "it${(0..1000).random()}"
            map += "it" to itName
            map += "mapped" to listType.toCodeBlock(itName)

            mapper = ".map { %it:L ->\n⇥%mapped:L\n⇤}"
        }

        return CodeBlock.builder()
            .addNamed(
                "SizedList(" +
                    "\n⇥n = %size:L," +
                    "\nlist = %propertyName:L$mapper," +
                    "\ndefault = %default:L\n⇤)",
                map
            ).build()
    }
}
