package com.ing.zkflow.annotated

import com.ing.zkflow.ASCII
import com.ing.zkflow.Size
import com.ing.zkflow.ZKP

@ZKP
data class WrapsSet(
    val set: @Size(5) Set<@ASCII(10) String> = setOf("test")
)
