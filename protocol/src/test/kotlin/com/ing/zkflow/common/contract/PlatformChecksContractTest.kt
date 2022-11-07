package com.ing.zkflow.common.contract

import com.ing.zkflow.annotations.Size
import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContract
import com.ing.zkflow.common.serialization.CommandDataSerializerRegistry
import com.ing.zkflow.common.serialization.ContractStateSerializerRegistry
import com.ing.zkflow.common.versioning.VersionedContractStateGroup
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.register
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.ContractState
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Random

/**
 * This test confirms that relevant checks from [net.corda.core.internal.Verifier.verify] are implemented in the generated Zinc code
 */
class PlatformChecksContractTest {
    private val services = MockServices(listOf("com.ing.zkflow"))
    private val alice = TestIdentity.fresh("Alice").party.anonymise()
    private val bob = TestIdentity.fresh("Bobby").party.anonymise()
    private val aliceAsset = ZKTestState(alice)
    private val bobAsset = aliceAsset.copy(owner = bob)

    /**
     * [net.corda.core.internal.Verifier.checkNoNotaryChange]
     */
    @Test
    fun `checkNoNotaryChange checks implemented`() {
        services.zkLedger {
            transaction {
                val wrongNotary = TestIdentity.fresh("WrongNotary").party
                input(LocalZKContract.PROGRAM_ID, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, null, wrongNotary, null, AlwaysAcceptAttachmentConstraint, bobAsset)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveAnyToPrivate())
                `fails with`("Found unexpected notary change in transaction")
            }
        }
    }

    /**
     * [net.corda.core.internal.Verifier.checkEncumbrancesValid]
     */
    @Test
    fun `checkEncumbrancesValid checks for inputs implemented`() {
        val notary = TestIdentity.fresh("ledger notary").party
        services.zkLedger(notary = notary) {
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Encumbered State 1", 1, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Encumbering State 1", SomeOtherZKState())
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivateEncumbered())
                verifies()
            }
            transaction {
                output(LocalZKContract.PROGRAM_ID, "Encumbered State 2", 1, aliceAsset)
                output(LocalZKContract.PROGRAM_ID, "Encumbering State 2", SomeOtherZKState())
                command(listOf(alice.owningKey), LocalZKContract.CreatePrivateEncumbered())
                verifies()
            }
            transaction {
                timeWindow(Instant.now())
                output(LocalZKContract.PROGRAM_ID, retrieveOutputStateAndRef(ContractState::class.java, "Encumbered State 1").state.data)
                command(listOf(alice.owningKey, bob.owningKey), LocalZKContract.MoveFullyPrivate())
                input("Encumbered State 1")
                `fails with`("Missing required encumbrance in inputs")
            }
            transaction {
                output(LocalZKContract.PROGRAM_ID, retrieveOutputStateAndRef(ContractState::class.java, "Encumbered State 1").state.data)
                command(listOf(alice.owningKey), LocalZKContract.UnencumberPrivate())
                input("Encumbered State 1")
                tweak {
                    input("Encumbering State 2")
                    `fails with`("Missing required encumbrance in inputs")
                }
                input("Encumbering State 1")
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
        val PROGRAM_ID: ContractClassName = this::class.java.enclosingClass.canonicalName
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
