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
data class Wraps3rdPartyNullable(
    val myList: @Size(2) List<@Size(2) List<@Default<OutOfReach>(DefaultOutOfReach::class) @Via<OutOfReachSurrogate> OutOfReach?>?>? = null
)

object DefaultOutOfReach : DefaultProvider<OutOfReach> {
    override val default = OutOfReach(5)
}

object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
    override fun from(original: OutOfReach) = OutOfReachSurrogate(original.value)
}

@ZKPSurrogate(ConverterOutOfReach::class)
class OutOfReachSurrogate(val value: Int) : Surrogate<OutOfReach> {
    override fun toOriginal() = OutOfReach(value)
}

data class OutOfReach(val value: Int)
