package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsManyNullables(
    val myList: @Size(5) List<@Size(5) List<@Size(5) List<Int?>?>?>? = null
)
