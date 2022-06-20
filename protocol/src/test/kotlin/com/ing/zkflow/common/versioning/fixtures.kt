package com.ing.zkflow.common.versioning

import com.ing.zkflow.common.serialization.SerializerRegistry
import com.ing.zkflow.serialization.register
import kotlinx.serialization.Serializable
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

interface TestFamily : Versioned

data class UnknownState(
    val value: Int
) : ContractState {
    override val participants: List<AbstractParty>
        get() = TODO("Not yet implemented")
}

@Serializable
data class TestStateV1(
    val value: Int
) : TestFamily, ContractState {
    override val participants: List<AbstractParty>
        get() = TODO("Not yet implemented")
}

@Serializable
data class TestStateV2(
    val value: Int
) : TestFamily, ContractState {
    constructor(previous: TestStateV1) : this(previous.value)
    override val participants: List<AbstractParty>
        get() = TODO("Not yet implemented")
}

@Serializable
data class TestStateV3(
    val value: Int
) : TestFamily, ContractState {
    constructor(previous: TestStateV2) : this(previous.value)
    override val participants: List<AbstractParty>
        get() = TODO("Not yet implemented")
}

val testFamily = VersionFamily(
    TestFamily::class,
    listOf(
        TestStateV1::class,
        TestStateV2::class,
        TestStateV3::class,
    )
)

object TestContractStateFamiliesRetriever : VersionFamiliesRetriever {
    override val families: List<VersionFamily> = listOf(
        testFamily
    )
}

private class TestSerializerRegistry : SerializerRegistry<ContractState>()

val testFamilyRegistry = VersionFamilyRegistry(
    TestContractStateFamiliesRetriever,
    TestSerializerRegistry().apply {
        register(TestStateV1::class, TestStateV1.serializer())
        register(TestStateV2::class, TestStateV2.serializer())
        register(TestStateV3::class, TestStateV3.serializer())
    }
)
