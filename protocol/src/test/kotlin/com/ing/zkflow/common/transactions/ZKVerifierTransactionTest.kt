package com.ing.zkflow.common.transactions

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.zkp.ZKFlow
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import com.ing.zkflow.common.zkp.metadata.packageName
import com.ing.zkflow.serialization.CommandDataSerializerMap
import com.ing.zkflow.testing.fixed
import com.ing.zkflow.testing.fixtures.contract.TestContract
import com.ing.zkflow.testing.fixtures.contract.TestContract.TestState
import com.ing.zkflow.testing.withCustomSerializationEnv
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import org.junit.jupiter.api.Test
import java.io.File

class ZKVerifierTransactionTest {

    private val services = MockServices(
        listOfNotNull(TestContract.PROGRAM_ID.packageName),
        TestIdentity.fixed("ServiceHub"),
        testNetworkParameters(minimumPlatformVersion = ZKFlow.REQUIRED_PLATFORM_VERSION),
    )
    private val notary = TestIdentity.fixed("Notary").party
    private val alice = TestIdentity.fixed("Alice").party.anonymise()

    @Test
    fun `test filtering outputs 1`() {
        withCustomSerializationEnv {

            @Serializable
            class TestZKCommand : ZKCommandData {

                @Transient
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    circuit {
                        buildFolder =
                            File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move")
                    }
                    outputs {
                        private(TestState::class) at 1
                    }
                    numberOfSigners = 2
                }

                init {
                    CommandDataSerializerMap.register(this::class)
                }
            }

            val publicOutput = TestState(alice, 0)
            val privateOutput = TestState(alice, 1)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(publicOutput) // at 0
            txbuilder.addOutputState(privateOutput) // at 1
            val wtx = txbuilder.toWireTransaction(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 2
            vtx.outputs.size shouldBe 1

            vtx.outputs.map { it.data } shouldContain publicOutput
            vtx.outputs.map { it.data } shouldNotContain privateOutput
        }
    }

    @Test
    fun `test filtering outputs 2`() {
        withCustomSerializationEnv {

            @Serializable
            class TestZKCommand : ZKCommandData {

                @Transient
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    circuit {
                        buildFolder =
                            File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move")
                    }
                    outputs {
                        private(TestState::class) at 0
                    }
                    numberOfSigners = 2
                }

                init {
                    CommandDataSerializerMap.register(this::class)
                }
            }

            val privateOutput = TestState(alice, 0)
            val publicOutput1 = TestState(alice, 1)
            val publicOutput2 = TestState(alice, 2)
            val publicOutput3 = TestState(alice, 3)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(privateOutput) // at 0
            txbuilder.addOutputState(publicOutput1) // at 1
            txbuilder.addOutputState(publicOutput2) // at 2
            txbuilder.addOutputState(publicOutput3) // at 3
            val wtx = txbuilder.toWireTransaction(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 4
            vtx.outputs.size shouldBe 3

            vtx.outputs.map { it.data } shouldContain publicOutput1
            vtx.outputs.map { it.data } shouldContain publicOutput2
            vtx.outputs.map { it.data } shouldContain publicOutput3
            vtx.outputs.map { it.data } shouldNotContain privateOutput
        }
    }

    @Test
    fun `test filtering outputs 3`() {
        withCustomSerializationEnv {

            @Serializable
            class TestZKCommand : ZKCommandData {

                @Transient
                override val metadata: ResolvedZKCommandMetadata = commandMetadata {
                    circuit {
                        buildFolder =
                            File("${System.getProperty("user.dir")}/../zinc-platform-sources/build/circuits/move")
                    }
                    outputs {
                        private(TestState::class) at 2
                        mixed(TestState::class) at 3
                        private(TestState::class) at 4
                    }
                    numberOfSigners = 2
                }

                init {
                    CommandDataSerializerMap.register(this::class)
                }
            }

            val publicOutput0 = TestState(alice, 0)
            val publicOutput1 = TestState(alice, 1)
            val privateOutput2 = TestState(alice, 2)
            val publicOutput3 = TestState(alice, 3)
            val privateOutput4 = TestState(alice, 4)

            val txbuilder = ZKTransactionBuilder(notary)
            txbuilder.addCommand(TestZKCommand(), alice.owningKey)
            txbuilder.addOutputState(publicOutput0) // at 0
            txbuilder.addOutputState(publicOutput1) // at 1
            txbuilder.addOutputState(privateOutput2) // at 2
            txbuilder.addOutputState(publicOutput3) // at 3
            txbuilder.addOutputState(privateOutput4) // at 4
            val wtx = txbuilder.toWireTransaction(services)
            val proofs = mapOf<ZKCommandClassName, Proof>()
            val vtx = ZKVerifierTransaction.fromWireTransaction(wtx, proofs)

            vtx.verify()

            vtx.outputHashes.size shouldBe 5
            vtx.outputs.size shouldBe 3

            vtx.outputs.map { it.data } shouldContain publicOutput0
            vtx.outputs.map { it.data } shouldContain publicOutput1
            vtx.outputs.map { it.data } shouldContain publicOutput3
            vtx.outputs.map { it.data } shouldNotContain privateOutput2
            vtx.outputs.map { it.data } shouldNotContain privateOutput4
        }
    }
}

val mockSerializers = run {}
