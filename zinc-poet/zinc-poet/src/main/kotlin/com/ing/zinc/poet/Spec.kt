package com.ing.zinc.poet

/**
 * [ZincFileItem]s represent zinc code that can be rendered into a [ZincFile].
 */
interface ZincFileItem {
    /**
     * Render this [ZincFileItem] for inclusion in a [ZincFile].
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
}

/**
 * A special [ZincType] that represents the `Self` zinc keyword.
 */
object Self : ZincType {
    private const val SELF_REFERENCE = "Self"
    override fun getId(): String = SELF_REFERENCE
}

data class Indentation(
    val spaces: Int
) {
    companion object {
        val Int.spaces: Indentation
            get() = Indentation(this)

        val Int.tabs: Indentation
            get() = Indentation(this * 4)
    }
}

/**
 * Convenience function to fix indentation problems when using [trimIndent] for multi-line strings.
 */
fun String.indent(indentation: Indentation): String {
    return replace("\n", "\n${"".padStart(indentation.spaces)}")
}

/**
 * This marker provides scope control for the DSL.
 * For more information see: https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker
 */
@DslMarker
annotation class ZincDslMarker
