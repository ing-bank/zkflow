package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsSet(
    val set: @Size(5) Set<@UTF8(10) String> = setOf("test")
)
