package com.ing.zkflow.common.contract

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.util.tryNonFailing
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Random
import kotlin.reflect.jvm.jvmName

class StateVisibilityContractTest {
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
                verifies()
            }
            transaction {
                input("Alice's Private Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                timeWindow(Instant.now())
                verifies()
            }
            verifies()
        }
    }

    @Test
    fun `Move any to private - fails for invalid number of signers`() {
        // TODO Even with the `fails with` clause this still throws an exception
        val ex = shouldThrow<ResolvedZKCommandMetadata.IllegalTransactionStructureException> {
            services.zkLedger {
                transaction {
                    input(LocalContract.PROGRAM_ID, aliceAsset)
                    output(LocalContract.PROGRAM_ID, bobAsset)
                    command(listOf(alice.owningKey), LocalContract.MoveAnyToPrivate())
                    `fails with`("Expected '2' signers for command ${LocalContract.MoveAnyToPrivate::class.jvmName}, but found '1'.")
                }
            }
        }
        ex.message shouldBe "Transaction does not match expected structure: Expected '2' signers for command ${LocalContract.MoveAnyToPrivate::class.jvmName}, but found '1'."
    }

    @Test
    fun `Move any to private`() {
        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveAnyToPrivate())
                verifies()
            }
        }
    }

    @Test
    fun `Move private to public`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePrivate())
                verifies()
            }
            val wtx = transaction {
                input("Alice's Private Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MovePrivateToPublic())
                timeWindow(Instant.now())
                verifies()
            }

            verifies()

            // Confirm tht the output is indeed in the list of public outputs of the MovePrivateToPublic transaction
            val publicOutputSerializedBytes =
                this.zkVerifierTransactionStorage.getTransaction(wtx.id)?.tx?.publicComponents(ComponentGroupEnum.OUTPUTS_GROUP)?.get(0)
            publicOutputSerializedBytes shouldNotBe null
        }
    }

    @Test
    fun `tx expects a private input utxo - fails on explicitly public input utxo`() {
        services.zkLedger {
            transaction {
                output(LocalContract.PROGRAM_ID, "Alice's Explicitly Public Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalContract.CreatePublicExplicitly())
                verifies()
            }
            transaction {
                input("Alice's Explicitly Public Asset")
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                val alicesPublicAssetRef = retrieveOutputStateAndRef(ZKContractState::class.java, "Alice's Explicitly Public Asset").ref
                `fails with`("UTXO for StateRef '$alicesPublicAssetRef' should be private, but it is public")
            }
        }
    }

    @Test
    fun `tx expects a private input utxo - fails on implicitly public input utxo`() {
        services.zkLedger {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.MoveFullyPrivate())
                `fails with`("should be private, but it is public")
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
                verifies()
            }
        }
    }
}

/**
 * Note that in addition to the @ZKP annotation, the classes and properties below need to be annotated with @Serializable.
 * This is because the ZKFlow annotation processors are not configured to run on the `protocol` module and therefore no @Serializable annotations are generated.
 * Because contract tests run with the custom BFL serializer, @Serializable annotations are required.
 * Tests that need these annotations to be generated from @ZKP annotations should be moved to the `integrations-tests` module.
 */
class LocalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.common.contract.LocalContract"
    }

    override fun verify(tx: LedgerTransaction) {}

    @Serializable
    @ZKP
    class CreatePublicExplicitly : ZKCommandData, Versioned {
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
    @ZKP
    class CreatePrivate : ZKCommandData, Versioned {
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
    @ZKP
    class MovePublic : CommandData {
        init {
            tryNonFailing {
                BFLSerializationScheme.Companion.CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    @ZKP
    class MoveAnyToPrivate : ZKCommandData, Versioned {
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
    @ZKP
    class MovePrivateToPublic : ZKCommandData, Versioned {
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
                public(ZKTestState::class) at 0
            }
            numberOfSigners = 2
            timeWindow = true
            notary = true
            command = true
            networkParameters = true
        }
    }

    @Serializable
    @ZKP
    class MoveFullyPrivate : ZKCommandData, Versioned {
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
            timeWindow = true
            notary = true
            command = true
            networkParameters = true
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
@ZKP
@BelongsToContract(LocalContract::class)
data class ZKTestState(
    val owner: @Serializable(with = OwnerSerializer::class) AnonymousParty,
    val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
) : ZKContractState, Versioned {
    private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

    @Transient
    override val participants: @Size(1) List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

    init {
        tryNonFailing {
            BFLSerializationScheme.Companion.ContractStateSerializerRegistry.register(this::class, serializer())
        }
    }
}
