package com.ing.zkflow.common.versioning

import com.ing.zkflow.util.STUB_FOR_TESTING
import kotlinx.serialization.Serializable
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty

interface TestFamily : VersionedContractStateGroup
interface UnregisteredFamily : VersionedContractStateGroup

data class UnknownState(
    val value: Int
) : ContractState {
    override val participants: List<AbstractParty>
        get() = STUB_FOR_TESTING()
}

@Serializable
data class TestStateV1(
    val value: Int
) : TestFamily, ContractState {
    override val participants: List<AbstractParty>
        get() = STUB_FOR_TESTING()
}

@Serializable
data class TestStateV2(
    val value: Int
) : TestFamily, ContractState {
    constructor(previous: TestStateV1) : this(previous.value)
    override val participants: List<AbstractParty>
        get() = STUB_FOR_TESTING()
}

@Serializable
data class TestStateV3(
    val value: Int
) : TestFamily, ContractState {
    constructor(previous: TestStateV2) : this(previous.value)
    override val participants: List<AbstractParty>
        get() = STUB_FOR_TESTING()
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

val testFamilyRegistry = VersionFamilyRegistry(TestContractStateFamiliesRetriever)
