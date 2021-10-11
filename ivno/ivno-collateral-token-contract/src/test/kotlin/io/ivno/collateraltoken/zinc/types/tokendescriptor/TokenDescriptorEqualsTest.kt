package io.ivno.collateraltoken.zinc.types.tokendescriptor

import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenDescriptor
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.ivno.collateraltoken.zinc.types.tokenDescriptor
import io.ivno.collateraltoken.zinc.types.tokenDescriptorWithOtherName
import io.ivno.collateraltoken.zinc.types.tokenDescriptorWithOtherSymbol
import kotlinx.serialization.json.buildJsonObject
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
}
