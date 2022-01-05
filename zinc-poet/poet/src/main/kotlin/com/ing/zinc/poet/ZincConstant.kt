package com.ing.zinc.poet

import com.ing.zinc.poet.Indentation.Companion.spaces
import com.ing.zinc.poet.ZincComment.Companion.zincComment

interface ZincConstant : ZincFileItem {
    fun getName(): String
    fun getType(): ZincType?
    fun getInitialization(): String
    fun getComment(): ZincComment?

    @ZincDslMarker
    class Builder {
        var name: String? = null
        var type: ZincType? = null
        var initialization: String? = null
        var comment: String? = null

        fun build(): ZincConstant = ImmutableZincConstant(
            requireNotNull(name) { "Required value `name` is null." },
            type,
            requireNotNull(initialization) { "Required value `initialization` is null." },
            comment,
        )
    }

    companion object {
        private data class ImmutableZincConstant(
            private val name: String,
            private val type: ZincType?,
            private val initialization: String,
            private val comment: String?
        ) : ZincConstant {
            override fun getName(): String = name
            override fun getType(): ZincType? = type
            override fun getInitialization(): String = initialization
            override fun getComment(): ZincComment? = comment?.let { zincComment(it) }
            override fun generate(): String {
                val typeString = getType()?.let {
                    ": ${it.getId()}"
                } ?: ""
                val commentString = getComment()?.let { "${it.generate()}\n" } ?: ""
                return commentString + if (getInitialization().contains("\n")) {
                    """
                        const ${getName()}$typeString
                            = ${getInitialization().indent(28.spaces)};
                    """.trimIndent()
                } else {
                    "const ${getName()}$typeString = ${getInitialization()};"
                }
            }
        }

        fun zincConstant(init: Builder.() -> Unit): ZincConstant = Builder().apply(init).build()
    }
}
