package com.ing.zkflow.annotated

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.Default
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.Surrogate
import com.ing.zkflow.Via
import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate

@ZKP
data class Wraps3rdPartyWithStar(
    val myList: @Size(2) List<@Size(2) List<@Default<OutOfReachStar>(DefaultOutOfReachStar::class) @Via<OutOfReachStarSurrogate> OutOfReachStar?>?>? = null
)

object DefaultOutOfReachStar : DefaultProvider<OutOfReachStar> {
    override val default = OutOfReachStar(setOf(1))
}

object ConverterOutOfReachStar : ConversionProvider<OutOfReachStar, OutOfReachStarSurrogate> {
    override fun from(original: OutOfReachStar) = OutOfReachStarSurrogate(
        original.value.map {
            it as? Int ?: error("Only Ints are accepted")
        }.toSet()
    )
}

@ZKPSurrogate(ConverterOutOfReachStar::class)
class OutOfReachStarSurrogate(val value: @Size(2) Set<Int>) : Surrogate<OutOfReachStar> {
    override fun toOriginal() = OutOfReachStar(value)
}

data class OutOfReachStar(val value: Set<*>)
