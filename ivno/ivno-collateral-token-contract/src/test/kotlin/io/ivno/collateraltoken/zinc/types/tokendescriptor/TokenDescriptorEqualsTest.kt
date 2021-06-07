package io.ivno.collateraltoken.zinc.types.tokendescriptor

import com.ing.zknotary.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.api.Test

class TokenDescriptorEqualsTest {
    private val zincZKService = getZincZKService<TokenDescriptorEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(tokenDescriptor, tokenDescriptor, true)
    }

    @Test
    fun `tokenDescriptors with different symbols should not be equal`() {
        performEqualityTest(tokenDescriptor, tokenDescriptorWithOtherSymbol, false)
    }

    @Test
    fun `tokenDescriptors with different names should not be equal`() {
        performEqualityTest(tokenDescriptor, tokenDescriptorWithOtherName, false)
    }

    private fun performEqualityTest(
        left: TokenDescriptor,
        right: TokenDescriptor,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val tokenDescriptor: TokenDescriptor = TokenDescriptor("Prince", CordaX500Name.parse("O=tafkap,L=New York,C=US"))
        val tokenDescriptorWithOtherSymbol = TokenDescriptor("Tafkap", CordaX500Name.parse("O=tafkap,L=New York,C=US"))
        val tokenDescriptorWithOtherName = TokenDescriptor("Prince", CordaX500Name.parse("O=prince,L=New York,C=US"))
    }
}
