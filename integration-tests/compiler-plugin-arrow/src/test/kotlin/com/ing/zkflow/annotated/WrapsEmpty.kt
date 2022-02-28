package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP

@ZKP
class WrapsEmpty {
    override fun hashCode(): Int = 1

    override fun equals(other: Any?): Boolean = when (other) {
        is WrapsEmpty -> true
        else -> false
    }
}
