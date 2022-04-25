package com.ing.zinc.poet

/**
 * Represents an item that can be converted to zinc code as a String.
 */
interface ZincGeneratable {
    /**
     * Convert to string representation.
     */
    fun generate(): String
}

/**
 * Represents a zinc type.
 *
 * [ZincType]s can be composed into more complex types, for example with [ZincArray] or [ZincStruct].
 */
interface ZincType {
    /**
     * Returns the identifier of the [ZincType] as used in actual zinc code.
     */
    fun getId(): String

    companion object {
        /**
         * A [ZincType] with the given [id].
         * This can be used to 'import' existing structs, enums or type definitions into other zinc-poet entities.
         * Before using this, always investigate whether any of the other [ZincType] implementations can be used.
         * E.g. for primitive types always use [ZincPrimitive].
         */
        data class ZincTypeIdentifier(
            private val id: String
        ) : ZincType {
            override fun getId(): String = id
        }

        fun id(id: String) = ZincTypeIdentifier(id)
    }

    /**
     * A special [ZincType] that represents the `Self` zinc keyword.
     */
    object Self : ZincType {
        private const val SELF_REFERENCE = "Self"
        override fun getId(): String = SELF_REFERENCE
    }
}

/**
 * Abstraction for [ZincFunction] and [ZincMethod].
 */
interface ZincInvokeable : ZincGeneratable {
    fun getName(): String
    fun getParameters(): List<ZincParameter>
    fun getReturnType(): ZincType
    fun getBody(): String

    /**
     * Optional comment to be placed before the function
     */
    fun getComment(): ZincComment?
}

/**
 * Represent an item that can be rendered into a [ZincFile].
 */
interface ZincFileItem : ZincGeneratable

/**
 * Convenience function to fix indentation problems when using [trimIndent] for multi-line strings.
 */
fun String.indent(indentation: Indentation): String = replace("\n", "\n" + indentation.indent)

data class Indentation(
    val indent: String
) {
    companion object {
        val Int.spaces: Indentation
            get() = Indentation(String(CharArray(this) { ' ' }))
    }
}

/**
 * This marker provides scope control for the DSL.
 * For more information see: https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker
 */
@DslMarker
annotation class ZincDslMarker
