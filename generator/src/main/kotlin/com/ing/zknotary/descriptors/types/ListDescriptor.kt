package com.ing.zknotary.descriptors.types

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.ing.zknotary.annotations.Sized
import com.ing.zknotary.annotations.WrappedList
import com.ing.zknotary.descriptors.Support
import com.ing.zknotary.descriptors.TypeDescriptor
import com.ing.zknotary.descriptors.describe
import com.ing.zknotary.util.expectAnnotation
import com.ing.zknotary.util.expectArgument
import com.ing.zknotary.util.getArgumentOrDefault
import com.squareup.kotlinpoet.CodeBlock

class ListDescriptor private constructor(private val size: Int, innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(
    WrappedList::class,
    innerDescriptors
) {
    companion object {
        fun fromKSP(list: KSType, support: Support): ListDescriptor {
            // List must be annotated with Sized.
            val sized = list.expectAnnotation<Sized>()

            // TODO Too many hardcoded things: property names and their types.
            val size = sized.expectArgument<Int>("size")
            val useDefault = sized.getArgumentOrDefault("useDefault", false)

            // List must have an inner type.
            val listType = list.arguments.single().type?.resolve()
                ?: error("List must have a type")

            val innerSupport = if (useDefault) {
                // Verify that the list type is a user class.
                val listClass = listType.declaration as? KSClassDeclaration
                    ?: error("$listType is not a user class and cannot be instantiated with a default value")

                // Verify this class has a default (empty) constructor.
                require(
                    listClass.getConstructors().any {
                        it.isPublic() && it.parameters.isEmpty()
                    }
                ) { "$listType must have a default (empty) constructor" }

                Support.Default
            } else {
                support
            }

            return ListDescriptor(size, listOf(listType.describe(innerSupport)))
        }
    }

    override val default: CodeBlock
        get() {
            val innerType = innerDescriptors.getOrNull(0)
                ?: error("WrappedList must declare type of its elements")

            return CodeBlock.of(
                "WrappedList( %L, %L )",
                size, innerType.default
            )
        }

    override fun toCodeBlock(propertyName: String): CodeBlock {
        val listType = innerDescriptors.getOrNull(0)
            ?: error("WrappedList must declare type of its elements")

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
                "WrappedList(" +
                    "\n⇥n = %size:L," +
                    "\nlist = %propertyName:L$mapper," +
                    "\ndefault = %default:L\n⇤)",
                map
            ).build()
    }
}
