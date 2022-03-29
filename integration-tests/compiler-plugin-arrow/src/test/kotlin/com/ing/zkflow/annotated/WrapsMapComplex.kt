package com.ing.zkflow.annotated

import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.UTF8
import com.ing.zkflow.annotations.ZKP

@ZKP
data class WrapsMapComplex(
    val map: @Size(11) Map<
        @UTF8(13) String,
        @Default<Baz>(Baz.Default::class) Baz
        > = mapOf("test" to Baz(1))
)

@ZKP
data class Baz(
    val id: Int
) {
    object Default : DefaultProvider<Baz> {
        override val default: Baz = Baz(0)
    }
}
