package io.ivno.collateraltoken.zinc.types.tokencontractcommandmove

import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenContract
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.ivno.collateraltoken.zinc.types.tokenContractCommandMove
import io.ivno.collateraltoken.zinc.types.tokenContractCommandMoveWithOtherContract
import io.ivno.collateraltoken.zinc.types.tokenContractCommandMoveWithoutContract
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class TokenContractCommandMoveEqualsTest {
    private val zincZKService = getZincZKService<TokenContractCommandMoveEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `attestationPointer equality test`(left: TokenContract.Command.Move, right: TokenContract.Command.Move, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: TokenContract.Command.Move,
        right: TokenContract.Command.Move,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Arguments.of(tokenContractCommandMove, tokenContractCommandMove, true),
            Arguments.of(tokenContractCommandMove, tokenContractCommandMoveWithOtherContract, false),
            Arguments.of(tokenContractCommandMove, tokenContractCommandMoveWithoutContract, false),
        )
    }
}
