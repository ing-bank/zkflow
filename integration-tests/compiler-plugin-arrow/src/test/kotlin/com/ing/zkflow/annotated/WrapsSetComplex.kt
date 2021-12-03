package com.ing.zkflow.annotated

import com.ing.zkflow.Default
import com.ing.zkflow.Size
import com.ing.zkflow.ZKP

@ZKP
data class WrapsSetComplex(
    val set: @Size(5) Set<@Default<Baz>(Baz.Default::class) Baz> = setOf(Baz(0))
)
