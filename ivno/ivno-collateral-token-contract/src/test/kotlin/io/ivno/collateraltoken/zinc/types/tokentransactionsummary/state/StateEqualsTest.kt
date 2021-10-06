package io.ivno.collateraltoken.zinc.types.tokentransactionsummary.state

import com.ing.zkflow.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.testing.getZincZKService
import io.dasl.contracts.v1.token.TokenTransactionSummary.State
import io.ivno.collateraltoken.zinc.types.state
import io.ivno.collateraltoken.zinc.types.stateWithDifferentAmounts
import io.ivno.collateraltoken.zinc.types.stateWithDifferentCommand
import io.ivno.collateraltoken.zinc.types.stateWithDifferentDescription
import io.ivno.collateraltoken.zinc.types.stateWithDifferentParticipants
import io.ivno.collateraltoken.zinc.types.stateWithDifferentTransactionId
import io.ivno.collateraltoken.zinc.types.stateWithDifferentTransactionTime
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class StateEqualsTest {
    private val zincZKService = getZincZKService<StateEqualsTest>()

    @ParameterizedTest
    @MethodSource("testData")
    fun `state equality test`(left: State, right: State, expected: Boolean) {
        performEqualityTest(left, right, expected)
    }

    private fun performEqualityTest(
        left: State,
        right: State,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    encodedSize =  EdDSASurrogate.ENCODED_SIZE,
                    isAnonymous = false,
                    scheme = Crypto.EDDSA_ED25519_SHA512
                )
            )
            put(
                "right",
                right.toJsonObject(
                    encodedSize =  EdDSASurrogate.ENCODED_SIZE,
                    isAnonymous = false,
                    scheme = Crypto.EDDSA_ED25519_SHA512
                )
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Arguments.of(state, state, true),
            Arguments.of(state, stateWithDifferentParticipants, false),
            Arguments.of(state, stateWithDifferentCommand, false),
            Arguments.of(state, stateWithDifferentAmounts, false),
            Arguments.of(state, stateWithDifferentDescription, false),
            Arguments.of(state, stateWithDifferentTransactionTime, false),
            Arguments.of(state, stateWithDifferentTransactionId, false),
        )

    }
}