package io.ivno.collateraltoken.zinc.types.accountaddress

import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.account.AccountAddress
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.api.Test

class AccountAddressEqualsTest {
    private val zincZKService = getZincZKService<AccountAddressEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(accountAddress, accountAddress, true)
    }

    @Test
    fun `accountAddress with different accountId should not be equal`() {
        performEqualityTest(accountAddress, accountAddressWithOtherAccountId, false)
    }

    @Test
    fun `accountAddress with different party should not be equal`() {
        performEqualityTest(accountAddress, accountAddressWithOtherParty, false)
    }

    private fun performEqualityTest(
        left: AccountAddress,
        right: AccountAddress,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val accountAddress = AccountAddress("Prince", CordaX500Name.parse("O=tafkap,L=New York,C=US"))
        val accountAddressWithOtherAccountId = AccountAddress("Tafkap", CordaX500Name.parse("O=tafkap,L=New York,C=US"))
        val accountAddressWithOtherParty = AccountAddress("Prince", CordaX500Name.parse("O=prince,L=New York,C=US"))
    }
}
