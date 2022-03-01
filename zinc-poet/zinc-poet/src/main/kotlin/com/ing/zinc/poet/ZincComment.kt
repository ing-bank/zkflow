package com.ing.zinc.poet

interface ZincComment : ZincFileItem {
    fun getComment(): String

    companion object {
        private data class ImmutableZincComment(
            private val comment: String
        ) : ZincComment {
            override fun getComment(): String = comment
            override fun generate(): String = "// ${getComment().replace("\n", "\n// ")}"
        }

        fun zincComment(comment: String): ZincComment = ImmutableZincComment(comment)
    }
}
