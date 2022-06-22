package com.ing.zkflow

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistry
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import io.kotest.matchers.shouldBe
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import org.junit.Test

class BFLSerializationRegistrationTest {
    @Test
    fun `Classes annotated with ZKP must be registered`() {
        // Successfully accessing the registration means that the serializer has been registered.
        ContractStateSerializerRegistry[MyState::class]
        CommandDataSerializerRegistry[MyCommand::class]
    }

    @Test
    fun `Classes annotated with ZKP should be discoverable by their id`() {
        ContractStateSerializerRegistry["${VersionedMyState::class.qualifiedName}0".hashCode()] shouldBe
            ContractStateSerializerRegistry[MyState::class]
    }
}

interface VersionedMyState : VersionedContractStateGroup

@ZKP
data class MyState(val i: Int) : ContractState, VersionedMyState {
    override val participants: List<AbstractParty> = emptyList()
}

@ZKP
class MyCommand : ZKCommandData {
    override val metadata: ResolvedZKCommandMetadata
        get() = TODO("Not yet implemented")
}
