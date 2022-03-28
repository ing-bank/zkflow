package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsMap(
    val map: @Size(5) Map<Int, @Size(10) String?> = mapOf(1 to "test", 2 to null)
)
