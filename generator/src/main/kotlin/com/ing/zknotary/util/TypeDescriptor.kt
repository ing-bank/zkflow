package com.ing.zknotary.util

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName

sealed class TypeDescriptor(val definition: ClassName, val typeKind: TypeKind) {
    abstract fun debug(logger: KSPLogger)

    abstract fun toCodeBlock(propertyName: String): CodeBlock
    abstract fun toCodeBlock(depth: Int): CodeBlock

    open val type: TypeName
        get() = definition

    open val default: CodeBlock
        get() =  typeKind.default()

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
            get() = typeKind.default(*innerDescriptors.map { it.default }.toTypedArray())

        override fun toCodeBlock(propertyName: String): CodeBlock {
            return typeKind.toCodeBlock(
                propertyName,
                *innerDescriptors.flatMap { listOf(
                    CodeBlock.of("%L", it.toCodeBlock(1)),
                    CodeBlock.of("%L", it.default)
                )}.toTypedArray()
            )
        }

        override fun toCodeBlock(depth: Int): CodeBlock {
            return typeKind.toCodeBlock(
                depth,
                *innerDescriptors.flatMap { listOf(
                    CodeBlock.of("%L", it.toCodeBlock(depth + 1)),
                    CodeBlock.of("%L", it.default)
                )}.toTypedArray()
            )
        }
    }

    class Trailing(
        definition: ClassName,
        typeKind: TypeKind
    ) : TypeDescriptor(definition, typeKind) {
        override fun debug(logger: KSPLogger) {
            logger.error("$definition : $default")
        }

        override fun toCodeBlock(propertyName: String): CodeBlock {
            return typeKind.toCodeBlock(propertyName)
        }

        override fun toCodeBlock(depth: Int): CodeBlock {
            return typeKind.toCodeBlock(depth)
        }
    }
}

sealed class TypeKind {
    abstract fun default(vararg arguments: CodeBlock): CodeBlock
    abstract fun toCodeBlock(instance: String, vararg arguments: CodeBlock): CodeBlock
    abstract fun toCodeBlock(depth: Int, vararg arguments: CodeBlock): CodeBlock

    class WrappedList_(val size: Int): TypeKind() {
        override fun default(vararg arguments: CodeBlock): CodeBlock {
            val innerDefault = arguments.getOrNull(0)
                ?: error("Cannot construct a wrapped list without a default value")

            return CodeBlock.of(
                "WrappedList( %L, %L )",
                size, innerDefault
            )
        }

        override fun toCodeBlock(instance: String, vararg arguments: CodeBlock): CodeBlock {
            val mapper = arguments.getOrNull(0) ?: error("ass")
            val default = arguments.getOrNull(1) ?: error("balls")

            return CodeBlock.of(
                "WrappedList(\n⇥n = %L,\nlist = %L.map { e0 ->\n⇥%L\n⇤},\ndefault = %L\n⇤)",
                size,
                instance,
                mapper,
                default
            )
        }

        override fun toCodeBlock(depth: Int, vararg arguments: CodeBlock): CodeBlock {
            val mapper = arguments.getOrNull(0) ?: error("ass")
            val default = arguments.getOrNull(1) ?: error("balls")

            return CodeBlock.of(
                "WrappedList(\n⇥n = %L,\nlist = e${depth - 1}.map { e$depth -> \n⇥%L\n⇤},\ndefault = %L\n⇤)",
                size,
                mapper,
                default
            )
        }

    }

    class Int_(val value: Int): TypeKind() {
        override fun default(vararg arguments: CodeBlock)
            = CodeBlock.of("%L", value)

        override fun toCodeBlock(instance: String, vararg arguments: CodeBlock): CodeBlock {
            return CodeBlock.of(instance)
        }

        override fun toCodeBlock(depth: Int, vararg arguments: CodeBlock): CodeBlock {
            return CodeBlock.of("e${depth - 1}")
        }
    }
}

