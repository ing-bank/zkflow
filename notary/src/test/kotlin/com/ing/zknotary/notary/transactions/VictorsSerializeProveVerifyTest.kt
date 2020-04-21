package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.serializer.VictorsZKInputSerializer
import com.ing.zknotary.common.zkp.ZincProverCLI
import com.ing.zknotary.common.zkp.ZincZKVerifierCLI
import net.corda.core.cordapp.CordappConfig
import net.corda.core.cordapp.CordappContext
import net.corda.core.crypto.sign
import net.corda.core.node.AppServiceHub
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class VictorsSerializeProveVerifyTest {

    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var mockServiceHub: AppServiceHub

    @Before
    fun setup() {
        val config = mock(CordappConfig::class.java)
        `when`(config.getString("proverKeyPath")).thenReturn("/path/to/prover/key")

        val appContext = mock(CordappContext::class.java)
        `when`(appContext.config).thenReturn(config)

        mockServiceHub = mock(AppServiceHub::class.java)
        `when`(mockServiceHub.getAppContext()).thenReturn(appContext)
    }

    @Test
    fun `Victor - prove and verify with valid tx is successful`() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(ledgerServices)
            val sigAlice = alice.keyPair.sign(wtx.id).bytes

            // Check out JsonZKInputSerializer for reference
            val witness = VictorsZKInputSerializer(mockServiceHub).serializeWitness(ltx, listOf(sigAlice))
            val instance = VictorsZKInputSerializer(mockServiceHub).serializeInstance(wtx.id)

            val proof = ZincProverCLI(mockServiceHub).prove(witness, instance)

            // No assertions required: this throws an exception on verification failure
            ZincZKVerifierCLI(mockServiceHub).verify(proof, instance)
        }
    }
}
