package com.example.rpc

import com.example.contract.cbdc.digitalEuro
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Suppress("UnusedPrivateMember")
@Tag("rpc")
class IssueTokenRpcTest {

    private val user = "user1"
    private val password = "test"

    private val issuer: SampleZKDappRPCClient = SampleZKDappRPCClient("127.0.0.1:10102", user, password)

    init {
        issuer.waitForConnection()
    }

    @Test
    fun `Issue a single token`() {
        issuer.create(digitalEuro(1.0, issuer.party(), TestIdentity.fresh("SomeHolder").party.anonymise()))
    }
}
