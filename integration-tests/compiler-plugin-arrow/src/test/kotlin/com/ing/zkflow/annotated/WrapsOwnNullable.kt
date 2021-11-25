package com.ing.zkflow.annotated

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.Size
import com.ing.zkflow.ZKP

@ZKP
data class WrapsOwnNullable(
    val myList: @Size(2) List<@Size(2) List<@Default<Flag>(DefaultFlag::class) Flag?>?>? = null
)

@ZKP
data class Flag(val value: Int)

object DefaultFlag : DefaultProvider<Flag> {
    override val default = Flag(1)
}
