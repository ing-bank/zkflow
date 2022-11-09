package com.ing.zkflow.common.contracts

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.node.services.InMemoryZKVerifierTransactionStorage
import com.ing.zkflow.common.node.services.ZKWritableVerifierTransactionStorage
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistry
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.register
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
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.Crypto
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
    private val zkVerifierTransactionStorage: ZKWritableVerifierTransactionStorage = InMemoryZKVerifierTransactionStorage()
    private val zkTransactionService = TestDSLMockZKTransactionService(services, zkVerifierTransactionStorage)

    @Test
    fun `Additional unchecked public only outputs are not allowed`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Alice's unchecked Public Asset", aliceAsset.copy(value = 99))
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivate())
                `fails with`("There should be no additional 'public only' outputs")
            }
        }
    }

    @Test
    fun `Additional unchecked public only outputs of different types are not allowed`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                command(listOf(alice.owningKey), LocalZKContract.CreateMultiplePublicAndPrivate())
                output(LocalZKContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Alice's Private some other state", SomeOtherZKState(1))
                output(LocalZKContract.PROGRAM_ID, "Alice's Public Asset", aliceAsset.copy(value = 2))
                output(LocalZKContract.PROGRAM_ID, "Alice's Public some other state", SomeOtherZKState(value = 3))
                output(LocalZKContract.PROGRAM_ID, "Alice's other Public some other state", SomeOtherZKState(value = 4))
                output(LocalNormalContract.PROGRAM_ID, LocalNormalContract.NormalTestState(bob, 5))
                tweak {
                    val state = SomeOtherZKState(value = 6)
                    output(LocalZKContract.PROGRAM_ID, "Alice's unchecked Public some other state", state)
                    `fails with`(
                        renderIllegalPublicOnlyStatesError("output", LocalZKContract::class, mapOf(state::class to listOf(state)))
                    )
                }
                tweak {
                    val state = aliceAsset.copy(value = 6)
                    output(LocalZKContract.PROGRAM_ID, "Alice's unchecked Public some other state", state)
                    `fails with`(
                        renderIllegalPublicOnlyStatesError("output", LocalZKContract::class, mapOf(state::class to listOf(state)))
                    )
                }
                tweak {
                    val state = SomeOtherZKState(value = 6)
                    val state2 = aliceAsset.copy(value = 7)
                    output(LocalZKContract.PROGRAM_ID, "Alice's unchecked Public some other state", state)
                    output(LocalZKContract.PROGRAM_ID, "Alice's unchecked Public Asset", state2)
                    `fails with`(
                        renderIllegalPublicOnlyStatesError(
                            "output",
                            LocalZKContract::class,
                            mapOf(state::class to listOf(state), state2::class to listOf(state2))
                        )
                    )
                }
                verifies()
            }
        }
    }

    @Test
    fun `Additional unchecked public only inputs are not allowed`() {
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                input(LocalZKContract.PROGRAM_ID, bobAsset)
                output(LocalZKContract.PROGRAM_ID, "Alice's Private Asset", aliceAsset)
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivate())
                `fails with`("There should be no additional 'public only' inputs")
            }
        }
    }

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
        }
    }

    @Test
    fun `Move any to private - fails for invalid number of signers`() {
        // Even with the `fails with` clause this still throws an exception
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
                tweak {
                    output(LocalZKContract.PROGRAM_ID, bobAsset.copy(value = 99))
                    `fails with`("There should be no additional 'public only' outputs")
                }
                verifies()
            }
            // Confirm that the output is indeed in the list of public outputs of the MovePrivateToPublic transaction
            val publicOutputSerializedBytes =
                this.zkVerifierTransactionStorage.getTransaction(wtx.id)?.tx?.publicComponents(ComponentGroupEnum.OUTPUTS_GROUP)?.get(1)
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
                val alicesPublicAssetRef = retrieveOutputStateAndRef(ContractState::class.java, "Alice's Explicitly Public Asset").ref
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
        val alicesNormalAsset = LocalNormalContract.NormalTestState(alice)
        services.zkLedger(zkService = zkTransactionService, zkVerifierTransactionStorage = zkVerifierTransactionStorage) {
            transaction {
                input(LocalNormalContract.PROGRAM_ID, alicesNormalAsset)
                output(LocalNormalContract.PROGRAM_ID, alicesNormalAsset.copy(owner = bob))
                command(listOf(alice.owningKey, bob.owningKey), LocalNormalContract.MovePublic())
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
class LocalNormalContract : Contract {
    companion object {
        const val PROGRAM_ID = "com.ing.zkflow.common.contracts.LocalNormalContract"
    }

    @Serializable
    @ZKP
    @BelongsToContract(LocalNormalContract::class)
    data class NormalTestState(
        val owner: @Serializable(with = OwnerSerializer::class) AnonymousParty,
        val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
    ) : ContractState, VersionedContractStateGroup {
        private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

        @Transient
        override val participants: @Size(1) List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

        init {
            tryNonFailing {
                ContractStateSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    @Serializable
    @ZKP
    class MovePublic : CommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    override fun verify(tx: LedgerTransaction) {
    }
}

class LocalZKContract : ZKContract, Contract {
    companion object {
        val PROGRAM_ID: ContractClassName = this::class.java.enclosingClass.canonicalName
    }

    @Serializable
    @ZKP
    class CreatePublicExplicitly : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                public(ZKTestState::class) at 0
            }
            numberOfSigners = 1
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class CreateMultiplePublicAndPrivate : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                private(ZKTestState::class) at 0
                private(SomeOtherZKState::class) at 1
                public(ZKTestState::class) at 2
                public(SomeOtherZKState::class) at 3
                public(SomeOtherZKState::class) at 4
            }
            numberOfSigners = 1
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class CreatePrivateEncumbered : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                private(ZKTestState::class) at 0
                private(SomeOtherZKState::class) at 1
            }
            numberOfSigners = 1
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class CreatePrivate : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            outputs {
                private(ZKTestState::class) at 0
            }
            numberOfSigners = 1
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class SplitAnyToPrivateAndPublic : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            notary = true
            inputs {
                any(ZKTestState::class) at 0
            }
            outputs {
                private(ZKTestState::class) at 0
                public(ZKTestState::class) at 1
            }
            numberOfSigners = 2
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class UnencumberPrivate : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
            }
        }

        @Transient
        override val metadata: ResolvedZKCommandMetadata = commandMetadata {
            inputs {
                private(ZKTestState::class) at 0
                private(SomeOtherZKState::class) at 1
            }
            outputs {
                private(ZKTestState::class) at 0
            }
            numberOfSigners = 1
            notary = true
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class MoveAnyToPrivate : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
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
            notary = true
        }

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class MovePrivateToPublic : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
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

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }

    @Serializable
    @ZKP
    class MoveFullyPrivate : ZKCommandData {
        init {
            tryNonFailing {
                CommandDataSerializerRegistry.register(this::class, serializer())
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

        override fun verifyPrivate(): String = """
            mod module_command_context;
            use module_command_context::CommandContext;
            
            fn verify(ctx: CommandContext) {
                // TODO
            }
        """.trimIndent()
    }
}

@Serializable
@ZKP
@BelongsToContract(LocalZKContract::class)
data class SomeOtherZKState(
    val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
) : ContractState, VersionedContractStateGroup {
    @Transient
    override val participants: @Size(1) List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = emptyList()

    init {
        tryNonFailing {
            ContractStateSerializerRegistry.register(this::class, serializer())
        }
    }
}

@Serializable
@ZKP
@BelongsToContract(LocalZKContract::class)
data class ZKTestState(
    val owner: @Serializable(with = OwnerSerializer::class) AnonymousParty,
    val value: @Serializable(with = IntSerializer::class) Int = Random().nextInt(1000)
) : ContractState, VersionedContractStateGroup {
    private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

    @Transient
    override val participants: @Size(1) List<@Serializable(with = AnonymousPartySerializer::class) AnonymousParty> = listOf(owner)

    init {
        tryNonFailing {
            ContractStateSerializerRegistry.register(this::class, serializer())
        }
    }
}
