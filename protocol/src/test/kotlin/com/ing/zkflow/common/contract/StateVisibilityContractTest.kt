package com.ing.zkflow.common.contract

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.interfaces.VerificationMode.MOCK
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Random

class SimpleContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()
    private val aliceAsset = ZKTestState(alice)
    private val bobAsset = aliceAsset.copy(owner = bob)

    @Test
    fun `Move private to private`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePrivate())
                verifies(MOCK)
            }
            transaction {
                input("Alice's Private Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                verifies(MOCK)
            }
            verifies(MOCK)
        }
    }

    @Test
    fun `Move any to private`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Explicitly Public Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePublicExplicitly())
                verifies(MOCK)
            }
            transaction {
                input("Alice's Explicitly Public Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveAnyToPrivate())
                verifies(MOCK)
            }
        }
    }

    // TODO: make sure that a tx like this does not create Proofs. It doesn't need them?
    @Test
    fun `Move private to public`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePrivate())
                verifies(MOCK)
            }
            transaction {
                input("Alice's Private Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MovePrivateToPublic())
                verifies(MOCK)
            }
            verifies(MOCK)
        }
    }

    @Test
    fun `tx expects a private input utxo - fails on explicitly public input utxo`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Explicitly Public Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePublicExplicitly())
                verifies(MOCK)
            }
            transaction {
                input("Alice's Explicitly Public Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                val alicesPublicAssetRef = retrieveOutputStateAndRef(ContractState::class.java, "Alice's Explicitly Public Asset").ref
                `fails with`("UTXO for StateRef '$alicesPublicAssetRef' should be private, but it is public")
            }
        }
    }

    @Test
    fun `tx expects a private input utxo - fails on implicitly public input utxo`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Implicitly Public Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePublicImplicitly())
                verifies(MOCK)
            }
            transaction {
                input("Alice's Implicitly Public Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                val alicesPublicAssetRef = retrieveOutputStateAndRef(ContractState::class.java, "Alice's Public Asset").ref
                `fails with`("UTXO for StateRef '$alicesPublicAssetRef' should be private, but it is public")
            }
        }
    }

    @Test
    fun `tx with one non-zkp public command`() {
        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MovePublic())
                verifies(MOCK)
            }
        }
    }

    @Test
    @Disabled("Enable and add surrogate for NonAnnotatableTestState once @ZKPSurrogate is implemented")
    fun `tx with nonannotatable state`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, NonAnnotatableTestState(alice))
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.CreateNonAnnotatableTestState())
                verifies(MOCK)
            }
        }
    }
}

class LocalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.common.contract.LocalContract"
    }

    override fun verify(tx: LedgerTransaction) {}

    @Serializable
    class CreateNonAnnotatableTestState : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    class CreatePublicImplicitly : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    class CreatePublicExplicitly : ZKCommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(ZKTestState::class) at 0
            }
            numberOfSigners = 1
        }
    }

    @Serializable
    class CreatePrivate : ZKCommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                private(ZKTestState::class) at 0
            }
            numberOfSigners = 1
        }
    }

    @Serializable
    class MovePublic : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    class MoveAnyToPrivate : ZKCommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs {
                any(ZKTestState::class) at 0
            }
            outputs {
                private(ZKTestState::class) at 0
            }
            numberOfSigners = 2
        }
    }

    @Serializable
    class MovePrivateToPublic : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    class MoveFullyPrivate : ZKCommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs {
                private(ZKTestState::class) at 0
            }
            outputs {
                private(ZKTestState::class) at 0
            }
            numberOfSigners = 2
        }
    }
}

@BelongsToContract(LocalContract::class)
data class NonAnnotatableTestState(
    val owner: AbstractParty,
    val value: Int = Random().nextInt(1000)
) : ContractState {
    override val participants: List<AbstractParty> = listOf(owner)
}

@Serializable
@BelongsToContract(LocalContract::class)
data class ZKTestState(
    val owner: @Serializable(with = OwnerSerializer::class) AnonymousParty,
    val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
) : ZKContractState {
    private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

    @Transient
    override val participants: @Size(1) List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

    init {
        tryNonFailing {
            BFLSerializationScheme.Companion.ContractStateSerializerRegistry.register(this::class, serializer())
        }
    }
}
