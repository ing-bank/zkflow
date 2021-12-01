package com.ing.zkflow.annotated

import com.ing.zkflow.Size
import com.ing.zkflow.ZKP

@ZKP
data class WrapsManyNullables(
    val myList: @Size(5) List<@Size(5) List<@Size(5) List<Int?>?>?>? = null
)
