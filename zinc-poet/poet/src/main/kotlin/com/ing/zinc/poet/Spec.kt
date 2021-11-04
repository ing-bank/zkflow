package com.ing.zinc.poet

/**
 * Mark as top-level element in a ZincFile.
 */
interface ZincFileItem {
    fun generate(): String
}

interface ZincType {
    fun getId(): String

    companion object {
        data class ZincTypeIdentifier(
            private val id: String
        ) : ZincType {
            override fun getId(): String = id
        }

        fun id(id: String) = ZincTypeIdentifier(id)
    }
}

object Self : ZincType {
    private const val selfReference = "Self"
    override fun getId(): String = selfReference
}

data class Indentation(
    val spaces: Int
) {
    companion object {
        val Int.spaces: Indentation
            get() = Indentation(this)
    }
}

/**
 * Convenience function to fix indentation problems when using [trimIndent] for multi-line strings.
 */
fun String.indent(indentation: Indentation): String {
    return replace("\n", "\n${"".padStart(indentation.spaces)}")
}
