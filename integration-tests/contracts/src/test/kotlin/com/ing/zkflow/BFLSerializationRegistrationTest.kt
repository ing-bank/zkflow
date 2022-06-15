package com.ing.zkflow

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.annotations.ZKPSurrogate
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.versioning.Versioned
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import org.junit.Test

class BFLSerializationRegistrationTest {
    @Test
    fun `Classes annotated with ZKPSurrogate must be registered`() {
        // Successfully accessing the registration means that the serializer has been registered.
        ContractStateSerializerRegistry[My3rdPartyClass::class]
        ContractStateSerializerRegistry[MyState::class]
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

interface VersionedMyState : Versioned

@ZKP
data class MyState(val i: Int) : ContractState, VersionedMyState {
    override val participants: List<AbstractParty> = emptyList()
}
