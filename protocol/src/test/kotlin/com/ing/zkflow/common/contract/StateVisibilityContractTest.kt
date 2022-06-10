package com.ing.zkflow.common.contract

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContract
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zkflow.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.services.TestDSLMockZKTransactionService
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
    private val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage = InMemoryZKVerifierTransactionStorage()
    private val zkTransactionService = TestDSLMockZKTransactionService(services, zkVerifierTransactionStorage)

    @Test
    fun `Move private to private`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            // services.zkLedger {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivate())
                verifies()
            }
            transaction {
                input("Alice's Private Asset")
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveFullyPrivate())
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
            services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
                transaction {
                    input(LocalZKContract.PROGRAM_ID, aliceAsset)
                    output(LocalZKContract.PROGRAM_ID, bobAsset)
                    command(listOf(alice.owningKey), LocalZKContract.MoveAnyToPrivate())
                    `fails with`("Expected '2' signers for command ${LocalZKContract.MoveAnyToPrivate::class.jvmName}, but found '1'.")
                }
            }
        }
        ex.message shouldBe "Transaction does not match expected structure: Expected '2' signers for command ${LocalZKContract.MoveAnyToPrivate::class.jvmName}, but found '1'."
    }

    @Test
    fun `Split any to private and public - confirm public visibility`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            val wtx = transaction {
                input(LocalZKContract.PROGRAM_ID, aliceAsset.copy(value = 3))
                output(LocalZKContract.PROGRAM_ID, bobAsset.copy(value = 2))
                output(LocalZKContract.PROGRAM_ID, bobAsset.copy(value = 1)) // the public one
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.SplitAnyToPrivateAndPublic())
                verifies()
            }
            // Confirm that the output is indeed in the list of public outputs of the MovePrivateToPublic transaction
            val publicOutputSerializedBytes =
                this.zkVerifierTransactionStorage.getTransaction(wtx.id)?.tx?.publicComponents(ComponentGroupEnum.OUTPUTS_GROUP)?.get(0)
            publicOutputSerializedBytes shouldNotBe null
        }
    }

    @Test
    fun `Move any to private`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                input(LocalZKContract.PROGRAM_ID, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveAnyToPrivate())
                verifies()
            }
        }
    }

    @Test
    fun `Move private to public`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivate())
                verifies()
            }
            val wtx = transaction {
                input("Alice's Private Asset")
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MovePrivateToPublic())
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
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Alice's Explicitly Public Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalZKContract.CreatePublicExplicitly())
                verifies()
            }
            transaction {
                input("Alice's Explicitly Public Asset")
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveFullyPrivate())
                val alicesPublicAssetRef = retrieveOutputStateAndRef(ZKContractState::class.java, "Alice's Explicitly Public Asset").ref
                `fails with`("UTXO for StateRef '$alicesPublicAssetRef' should be private, but it is public")
            }
        }
    }

    @Test
    fun `tx expects a private input utxo - fails on implicitly public input utxo`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                input(LocalZKContract.PROGRAM_ID, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveFullyPrivate())
                `fails with`("should be private, but it is public")
            }
        }
    }

    @Test
    fun `tx with one non-zkp public command`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                input(LocalZKContract.PROGRAM_ID, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MovePublic())
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
class LocalZKContract : ZKContract, Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.common.contract.LocalZKContract"
    }

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
    class SplitAnyToPrivateAndPublic : ZKCommandData, Versioned {
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
                public(ZKTestState::class) at 1
            }
            numberOfSigners = 2
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

@BelongsToContract(LocalZKContract::class)
data class NonAnnotatableTestState(
    val owner: AbstractParty,
    val value: Int = Random().nextInt(1000)
) : ContractState {
    override val participants: List<AbstractParty> = listOf(owner)
}

@Serializable
@ZKP
@BelongsToContract(LocalZKContract::class)
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
