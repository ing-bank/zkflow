package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

sealed class TypeDescriptor(val definition: ClassName, val typeKind: TypeKind) {
    abstract fun debug(logger: KSPLogger)

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
        override fun debug(logger: KSPLogger) {
            logger.error("(${(0..100).random()}) $definition")

            innerDescriptors.forEach { it.debug(logger) }
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
        override fun debug(logger: KSPLogger) {
            logger.error("(${(0..100).random()}) $definition : $default")
        }
    }
}

sealed class TypeKind {
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
            val typeFirst = innerDescriptors.getOrNull(0)
                ?: error("Pair<A, B> must declare type A")

            val typeSecond = innerDescriptors.getOrNull(1)
                ?: error("Pair<A, B> must declare type B")

            return CodeBlock.of(
                "Pair( %L, %L )",
                typeFirst.default, typeSecond.default
            )
        }

        override fun toCodeBlock(propertyName: String, innerDescriptors: List<TypeDescriptor>): CodeBlock {
            val firstType = innerDescriptors.getOrNull(0)
                ?: error("Pair requires type of its first element")
            val secondType = innerDescriptors.getOrNull(1)
                ?: error("Pair requires type of its second element")

            val map = mapOf(
                "propertyName" to propertyName,
                "first" to firstType.toCodeBlock("$propertyName.first"),
                "second" to secondType.toCodeBlock("$propertyName.second")
            )
            return CodeBlock.builder()
                .addNamed(
                    "Pair( %first:L, %second:L )",
                    map
                ).build()
        }
    }
}

