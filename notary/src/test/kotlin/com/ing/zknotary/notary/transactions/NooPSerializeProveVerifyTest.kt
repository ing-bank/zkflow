package com.ing.zknotary.notary.transactions

import com.ing.zknotary.common.serializer.NoopZKInputSerializer
import com.ing.zknotary.common.zkp.NoopZKProver
import com.ing.zknotary.common.zkp.NoopZKVerifier
import net.corda.core.cordapp.CordappConfig
import net.corda.core.cordapp.CordappContext
import net.corda.core.crypto.sign
import net.corda.core.node.AppServiceHub
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class NooPSerializeProveVerifyTest {

    private val alice = TestIdentity.fresh("alice")
    private val bob = TestIdentity.fresh("bob")

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice
    )

    private lateinit var mockServiceHub: AppServiceHub

    @Before
    fun setup() {
        val config = Mockito.mock(CordappConfig::class.java)
        Mockito.`when`(config.getString("proverKeyPath")).thenReturn("/path/to/prover/key")

        val appContext = Mockito.mock(CordappContext::class.java)
        Mockito.`when`(appContext.config).thenReturn(config)

        mockServiceHub = Mockito.mock(AppServiceHub::class.java)
        Mockito.`when`(mockServiceHub.getAppContext()).thenReturn(appContext)
    }

    @Test
    fun `Noop - prove and verify with valid tx is successful`() {
        ledgerServices.ledger {
            val wtx = moveTestsState(createTestsState(owner = alice), newOwner = bob)
            verifies()

            val ltx = wtx.toLedgerTransaction(ledgerServices)

            val sigAlice = alice.keyPair.sign(wtx.id).bytes

            val witness = NoopZKInputSerializer(mockServiceHub).serializeWitness(ltx, listOf(sigAlice))
            val instance = NoopZKInputSerializer(mockServiceHub).serializeInstance(wtx.id)

            val proof = NoopZKProver(mockServiceHub).prove(witness, instance)

            NoopZKVerifier(mockServiceHub).verify(proof, instance)
        }
    }
}
