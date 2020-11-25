package com.ing.zknotary.testcordapp.contracts

import com.ing.zknotary.testing.fixed
import net.corda.core.crypto.Crypto
import net.corda.core.utilities.loggerFor
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import kotlin.time.ExperimentalTime

@ExperimentalTime
class SomeContractTest {
    private val logger = loggerFor<SomeContractTest>()

    private val alice = TestIdentity.fixed("alice", Crypto.EDDSA_ED25519_SHA512)
    private val bob = TestIdentity.fixed("bob", Crypto.EDDSA_ED25519_SHA512)

    private val ledgerServices = MockServices(
        listOf("com.ing.zknotary.common.contracts"),
        alice,
        testNetworkParameters(minimumPlatformVersion = 6),
        bob
    )

    private val ledger = ledgerServices.ledger {}
}
