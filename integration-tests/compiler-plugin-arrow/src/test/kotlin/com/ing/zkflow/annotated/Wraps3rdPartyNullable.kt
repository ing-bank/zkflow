package com.ing.zkflow.annotated

import com.ing.zkflow.ConversionProvider
import com.ing.zkflow.DefaultProvider
import com.ing.zkflow.Resolver
import com.ing.zkflow.Size
import com.ing.zkflow.Surrogate
import com.ing.zkflow.ZKP

@ZKP
data class Wraps3rdPartyNullable(
    val myList: @Size(2) List<@Size(2) List<@Resolver<OutOfReach, OutOfReachSurrogate>(DefaultOutOfReach::class, ConverterOutOfReach::class) OutOfReach?>?>? = null
)

object DefaultOutOfReach : DefaultProvider<OutOfReach> {
    override val default = OutOfReach(5)
}

object ConverterOutOfReach : ConversionProvider<OutOfReach, OutOfReachSurrogate> {
    override fun from(original: OutOfReach) = OutOfReachSurrogate(original.value)
}

@ZKP
class OutOfReachSurrogate(val value: Int) : Surrogate<OutOfReach> {
    override fun toOriginal() = OutOfReach(value)
}

data class OutOfReach(val value: Int)
