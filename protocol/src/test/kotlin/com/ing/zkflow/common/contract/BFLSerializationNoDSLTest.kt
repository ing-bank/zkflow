package com.ing.zkflow.common.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.network.ZKAttachmentConstraintType
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.CommandDataSerializerRegistry
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ContractStateSerializerRegistry
import com.ing.zkflow.common.transactions.ZKTransactionBuilder
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.packageName
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.fixed
import com.ing.zkflow.testing.withCustomSerializationEnv
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.Test
import java.util.Random

class BFLSerializationNoDSLTest {
    private val services = MockServices(
        listOfNotNull(LocalContract.PROGRAM_ID.packageName),
        TestIdentity.fixed("ServiceHub"),
        testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION),
    )
    private val notary = TestIdentity.fixed("Notary").party
    private val alice = TestIdentity.fixed("Alice").party.anonymise()

    private val zkNetworkParameters = MockZKNetworkParameters(
        attachmentConstraintType = ZKAttachmentConstraintType.HashAttachmentConstraintType(),
        serializationSchemeId = BFLSerializationScheme.SCHEME_ID
    )

    @Test
    fun `Directly constructed tx must serialize and deserialize`() {
        withCustomSerializationEnv {
            val privateOutput = TestState(alice, 1)

            val txbuilder = ZKTransactionBuilder(notary, zkNetworkParameters)
            txbuilder.addCommand(LocalContract.Issue(), alice.owningKey)
            txbuilder.addOutputState(privateOutput) // at 0
            txbuilder.toWireTransaction(services)
        }
    }

    @Serializable
    @BelongsToContract(LocalContract::class)
    data class TestState(
        @Serializable(with = Owner::class) val owner: AnonymousParty,
        @Serializable(with = IntSerializer::class) val value: Int = Random().nextInt(1000)
    ) : ZKContractState {
        private object Owner : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

        @Transient
        override val participants: List<AnonymousParty> = listOf(owner)

        init {
            tryNonFailing {
                ContractStateSerializerRegistry.register(this::class, serializer())
            }
        }
    }

    class LocalContract : Contract {
        companion object {
            const val PROGRAM_ID = "com.ing.zkflow.common.contract.BFLSerializationNoDSLTest\$LocalContract"
        }

        override fun verify(tx: LedgerTransaction) {}

        @Serializable
        class Issue : ZKCommandData {
            init {
                tryNonFailing {
                    CommandDataSerializerRegistry.register(this::class, serializer())
                }
            }

            @Transient
            override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                outputs {
                    private(TestState::class) at 0
                }
                numberOfSigners = 2
            }
        }
    }
}
