package com.ing.zkflow.annotated

import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsNestedEmpty(
    val nested: WrapsEmpty = WrapsEmpty()
)
