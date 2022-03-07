package com.ing.zkflow.common.contract

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.contracts.ZKContractState
import com.ing.zkflow.common.serialization.BFLSerializationScheme
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.CommandDataSerializerRegistry
import com.ing.zkflow.common.serialization.BFLSerializationScheme.Companion.ContractStateSerializerRegistry
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.serialization.serializer.IntSerializer
import com.ing.zkflow.serialization.serializer.corda.AnonymousPartySerializer
import com.ing.zkflow.testing.dsl.VerificationMode
import com.ing.zkflow.testing.dsl.zkLedger
import com.ing.zkflow.testing.zkp.MockZKNetworkParameters
import com.ing.zkflow.util.tryNonFailing
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.Contract
import net.corda.core.crypto.Crypto
import net.corda.core.identity.AnonymousParty
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.util.Random

class BFLSerializationWithDSLTest {
    private val zkNetworkParameters = MockZKNetworkParameters(
        serializationSchemeId = BFLSerializationScheme.SCHEME_ID
    )

    @Test
    fun `Happy flow contract test must succeed`() {
        val services = MockServices(listOf("com.ing.zkflow"))
        val alice = TestIdentity.fresh("Alice").party.anonymise()
        val bob = TestIdentity.fresh("Bobby").party.anonymise()

        val aliceAsset = TestState(alice)

        services.zkLedger(zkNetworkParameters = zkNetworkParameters) {
            transaction {
                input(LocalContract.PROGRAM_ID, aliceAsset)
                output(LocalContract.PROGRAM_ID, aliceAsset.copy(owner = bob))
                command(listOf(alice.owningKey, bob.owningKey), LocalContract.Move())
                verifies(VerificationMode.MOCK)
            }
        }
    }

    @Serializable
    @BelongsToContract(LocalContract::class)
    data class TestState(
        @Serializable(with = OwnerSerializer::class) val owner: AnonymousParty,
        @Serializable(with = IntSerializer::class) val value: Int = Random().nextInt(1000)
    ) : ZKContractState {
        private object OwnerSerializer : AnonymousPartySerializer(Crypto.EDDSA_ED25519_SHA512.schemeNumberID)

        @Transient
        override val participants: List<AnonymousParty> = listOf(owner)

        init {
            tryNonFailing { ContractStateSerializerRegistry.register(this::class, serializer()) }
        }
    }

    class LocalContract : Contract {
        companion object {
            const val PROGRAM_ID = "com.ing.zkflow.common.contract.BFLSerializationWithDSLTest\$LocalContract"
        }

        override fun verify(tx: LedgerTransaction) {}

        @Serializable
        class Move : ZKCommandData {
            init {
                tryNonFailing { CommandDataSerializerRegistry.register(this::class, serializer()) }
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
}
