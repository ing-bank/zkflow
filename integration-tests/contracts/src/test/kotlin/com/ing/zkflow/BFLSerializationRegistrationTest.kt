package com.ing.zkflow

import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AnonymousParty
import org.junit.Test

class BFLSerializationRegistrationTest {
    @Test
    fun `Classes annotated with ZKPSurrogate must be registered`() {
        // Scheme must be instantiated, otherwise its companion object won't be computed.
        BFLSerializationScheme()

        // Successfully accessing the registration means that the serializer has been registered.
        BFLSerializationScheme.Companion.ContractStateSerializerRegistry[My3rdPartyClass::class]
    }
}

@ZKPSurrogate(MyConverter::class)
data class My3rdPartyClassSurrogate(
    val i: Int
) : Surrogate<My3rdPartyClass> {
    override fun toOriginal() = My3rdPartyClass(i)
}

data class My3rdPartyClass(val i: Int) : ContractState {
    override val participants = emptyList<AnonymousParty>()
}

object MyConverter : ConversionProvider<My3rdPartyClass, My3rdPartyClassSurrogate> {
    override fun from(original: My3rdPartyClass) = My3rdPartyClassSurrogate(original.i)
}
