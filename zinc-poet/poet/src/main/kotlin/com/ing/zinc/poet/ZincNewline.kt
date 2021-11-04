package com.ing.zinc.poet

interface ZincNewline : ZincFileItem {
    companion object {
        object ImmutableZincNewline : ZincNewline {
            override fun generate(): String = ""
        }

        fun zincNewline() = ImmutableZincNewline
    }
}
