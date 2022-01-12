package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsEnum(val option: Option = Option.FIRST)

@ZKP
enum class Option {
    FIRST,
    SECOND
}
