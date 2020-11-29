package com.ing.zknotary.util

import com.google.devtools.ksp.symbol.KSDeclaration
import com.ing.zknotary.annotations.WrappedList
import com.ing.zknotary.generator.log
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import kotlin.reflect.KClass

/**
 * `TypeDescriptor` enables description of types parametrized by other types.
 * This description is used to assemble types with required replacements
 * and to create corresponding default values.
 * For example, List<Pair<Int, Int>> is described as follows:
 *                          TypeDescriptor.List_
 *                          (inner descriptors)
 *                                  |
 *                          TypeDescriptor.Pair_
 *                          (inner descriptors)
 *          |----------------------|-------------------------------|
 *          |                                                      |
 *   TypeDescriptor.Int_                                    TypeDescriptor.Int_
 *
 * Each version of TypeDescriptor implements a bespoke functionality
 * to construct default values and to create values of the right type from a variable.
 *
 * In the previous example, examples of such functionality are:
 * - TypeDescriptor.List_ implements replacement of `List` to `WrappedList`
 * and maps contained elements into sized versions;
 * - TypeDescriptor.Pair_ constructs a pair making use of `first` and `second` fields.
 */


sealed class TypeDescriptor(
    val definition: ClassName,
    val innerDescriptors: List<TypeDescriptor> = listOf()
){
    constructor(clazz: KClass<*>, innerDescriptors: List<TypeDescriptor>) : this(
        ClassName(
            clazz.java.`package`.name,
            listOf(clazz.simpleName!!)
        ),
        innerDescriptors
    )

    companion object {
        val supported = listOf(
            Int::class.simpleName,
            Pair::class.simpleName,
            Triple::class.simpleName,
            List::class.simpleName
        )

        fun supports(typeName: String): Boolean =
            supported.contains(typeName)
    }

    abstract val default: CodeBlock
    abstract fun toCodeBlock(propertyName: String): CodeBlock

    val isCompound: Boolean = innerDescriptors.isNotEmpty()


    open val type: TypeName
        get() = definition.parameterizedBy(innerDescriptors.map { it.type })

    fun debug() {
        if (innerDescriptors.isEmpty()) {
            log?.error("(${(0..100).random()}) $definition : $default")
        } else {
            log?.error("(${(0..100).random()}) $definition")
            innerDescriptors.forEach { it.debug() }
        }
    }

    class List_(val size: Int, innerDescriptors: List<TypeDescriptor>): TypeDescriptor(
        WrappedList::class,
        innerDescriptors
    ) {
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

            if (listType.isCompound) {
                val itName = "it${(0..100).random()}"
                map += "it" to itName
                map += "mapped" to listType.toCodeBlock(itName)

                mapper = ".map { %it:L ->\n⇥%mapped:L\n⇤}"
            }

            return CodeBlock.builder()
                .addNamed(
                    "WrappedList("+
                        "\n⇥n = %size:L," +
                        "\nlist = %propertyName:L$mapper," +
                        "\ndefault = %default:L\n⇤)",
                    map
                ).build()
        }
    }

    // it's impossible to get to the package name for `Int` from `Int::class`
    // 1. Kotlin has not such native functionality,
    //    although I can resolve it from `Int::class.qualifiedName` (evaluates to `kotlin.Int`),
    //    but I don't want to hardcode such a thing.
    // 2. `KSDeclaration` allows to resolve package name, so I prefer
    //    taking an extra parameter providing the required functionality.
    class Int_(val value: Int, declaration: KSDeclaration): TypeDescriptor(
        ClassName(
            declaration.packageName.asString(),
            listOf(declaration.simpleName.asString()!!)
        )
    ) {
        override val type: TypeName
            get() = definition

        override val default = CodeBlock.of("%L", value)

        override fun toCodeBlock(propertyName: String)
            = CodeBlock.of(propertyName)
    }

    class Pair_(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Pair::class, innerDescriptors) {
        override val default: CodeBlock
            get() {
                val firstType = innerDescriptors.getOrNull(0)
                    ?: error("Pair<A, B> must declare type A")

                val secondType = innerDescriptors.getOrNull(1)
                    ?: error("Pair<A, B> must declare type B")

                return CodeBlock.of(
                    "Pair( %L, %L )",
                    firstType.default, secondType.default
                )
            }

        override fun toCodeBlock(propertyName: String): CodeBlock {
            val first = innerDescriptors.getOrNull(0)
                ?: error("Pair<A, B> must declare type A")
            val second = innerDescriptors.getOrNull(1)
                ?: error("Pair<A, B> must declare type B")

            val map = mapOf(
                "propertyName" to propertyName,
                "first" to first.toCodeBlock("$propertyName.first"),
                "second" to second.toCodeBlock("$propertyName.second")
            )

            return CodeBlock.builder()
                .addNamed(
                    "Pair( %first:L, %second:L )",
                    map
                ).build()
        }
    }

    class Triple_(innerDescriptors: List<TypeDescriptor>) : TypeDescriptor(Triple::class, innerDescriptors) {
        override val default: CodeBlock
            get() {
                val first = innerDescriptors.getOrNull(0)
                    ?: error("Triple<A, B, C> must declare type A")

                val second = innerDescriptors.getOrNull(1)
                    ?: error("Triple<A, B, C> must declare type B")

                val third = innerDescriptors.getOrNull(2)
                    ?: error("Triple<A, B, C> must declare type C")

                return CodeBlock.of(
                    "Triple( %L, %L, %L )",
                    first.default, second.default, third.default
                )
            }

        override fun toCodeBlock(propertyName: String): CodeBlock {
            val first = innerDescriptors.getOrNull(0)
                ?: error("Triple<A, B, C> must declare type A")

            val second = innerDescriptors.getOrNull(1)
                ?: error("Triple<A, B, C> must declare type B")

            val third = innerDescriptors.getOrNull(2)
                ?: error("Triple<A, B, C> must declare type C")

            val map = mapOf(
                "propertyName" to propertyName,
                "first" to first.toCodeBlock("$propertyName.first"),
                "second" to second.toCodeBlock("$propertyName.second"),
                "third" to third.toCodeBlock("$propertyName.third")
            )

            return CodeBlock.builder()
                .addNamed(
                    "Triple( %first:L, %second:L, %third:L )",
                    map
                ).build()
        }
    }
}

