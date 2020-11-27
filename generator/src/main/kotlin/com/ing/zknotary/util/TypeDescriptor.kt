package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger
import com.ing.zknotary.generator.log
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

sealed class TypeDescriptor(val definition: ClassName, val typeKind: TypeKind) {
    // TODO remove in the final version
    // this function uses a global logger, which will also be removed in the final version.
    abstract fun debug()

    open val type: TypeName
        get() = definition

    open val default: CodeBlock
        get() =  typeKind.default(listOf())

    open fun toCodeBlock(propertyName: String): CodeBlock {
        return typeKind.toCodeBlock(propertyName)
    }

    class Transient(
        definition: ClassName,
        private val innerDescriptors: List<TypeDescriptor>,
        typeKind: TypeKind
    ) : TypeDescriptor(definition, typeKind) {
        override fun debug() {
            log?.error("(${(0..100).random()}) $definition")

            innerDescriptors.forEach { it.debug() }
        }

        override val type: TypeName
            get() = definition.parameterizedBy(innerDescriptors.map { it.type })

        override val default: CodeBlock
            get() = typeKind.default(innerDescriptors)

        override fun toCodeBlock(propertyName: String): CodeBlock =
            typeKind.toCodeBlock(propertyName, innerDescriptors)
    }

    class Trailing(
        definition: ClassName,
        typeKind: TypeKind
    ) : TypeDescriptor(definition, typeKind) {
        override fun debug() {
            log?.error("(${(0..100).random()}) $definition : $default")
        }
    }
}

sealed class TypeKind {
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

    abstract fun default(innerDescriptors: List<TypeDescriptor>): CodeBlock
    abstract fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor> = listOf()): CodeBlock

    class WrappedList_(val size: Int): TypeKind() {
        override fun default(innerDescriptors: List<TypeDescriptor>): CodeBlock {
            val innerType = innerDescriptors.getOrNull(0)
                ?: error("WrappedList must declare type of its elements")

            return CodeBlock.of(
                "WrappedList( %L, %L )",
                size, innerType.default
            )
        }

        override fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor>): CodeBlock {
            val listType = innerDescriptors.getOrNull(0)
                ?: error("WrappedList must declare type of its elements")

            var map = mapOf(
                "propertyName" to propertyName,
                "size" to size,
                "default" to listType.default
            )
            var mapper = ""

            if (listType is TypeDescriptor.Transient) {
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

    class Int_(val value: Int): TypeKind() {
        override fun default(innerDescriptors: List<TypeDescriptor>)
            = CodeBlock.of("%L", value)

        override fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor>)
            = CodeBlock.of(propertyName)
    }

    object Pair_ : TypeKind() {
        override fun default(innerDescriptors: List<TypeDescriptor>): CodeBlock {
            val firstType = innerDescriptors.getOrNull(0)
                ?: error("Pair<A, B> must declare type A")

            val secondType = innerDescriptors.getOrNull(1)
                ?: error("Pair<A, B> must declare type B")

            return CodeBlock.of(
                "Pair( %L, %L )",
                firstType.default, secondType.default
            )
        }

        override fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor>): CodeBlock {
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

    object Triple_ : TypeKind() {
        override fun default(innerDescriptors: List<TypeDescriptor>): CodeBlock {
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

        override fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor>): CodeBlock {
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

