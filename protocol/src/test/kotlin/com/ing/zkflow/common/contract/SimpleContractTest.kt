package com.ing.zkflow.common.contract

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.util.Random

class SimpleContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()

    @Test
    fun `tx with one private command with private input and output`() {
        val aliceAsset = TestState(alice)

        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, aliceAsset.copy(owner = bob))
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                verifies(VerificationMode.MOCK)
            }
        }
    }

    @Test
    fun `tx with one public command`() {
        val aliceAsset = TestState(alice)

        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, aliceAsset.copy(owner = bob))
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MovePublic())
                verifies(VerificationMode.MOCK)
            }
        }
    }

    @Test
    fun `consume public state in private command`() {
        val aliceAsset = TestState(alice)
        val bobAsset = aliceAsset.copy(owner = bob)

        val charlie = TestIdentity.fresh("Charlie").party.anonymise()
        val charlieAsset = bobAsset.copy(owner = charlie)

        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, "Bob's Public Output", bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MovePublic())
                verifies(VerificationMode.MOCK)
            }
            transaction {
                input("Bob's Public Output")
                output(LocalContract.PROGRAM_ID, charlieAsset)
                command(listOf(bob.owningKey, charlie.owningKey), LocalContract.MovePrivateOutputOnly())
                verifies(VerificationMode.MOCK)
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
    class MovePublic : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    class MovePrivateOutputOnly : ZKCommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs {
                any(TestState::class) at 0
            }
            outputs {
                private(TestState::class) at 0
            }
            numberOfSigners = 2
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
                private(TestState::class) at 0
            }
            outputs {
                private(TestState::class) at 0
            }
            numberOfSigners = 2
        }
    }
}

@Serializable
@BelongsToContract(LocalContract::class)
data class TestState(
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
