package io.ivno.collateraltoken.zinc.types.ivnotokentype

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class IvnoTokenTypeEqualsTest {
    private val zincZKService = getZincZKService<IvnoTokenTypeEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(ivnoTokenType, ivnoTokenType, true)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different ivnoTokenType should not be equal`(testPair: Pair<IvnoTokenType, IvnoTokenType>) {
        performEqualityTest(testPair.first, testPair.second, false)
    }

    private fun performEqualityTest(
        left: IvnoTokenType,
        right: IvnoTokenType,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isAnonymous = false,
                    networkScheme = Crypto.EDDSA_ED25519_SHA512,
                    custodianEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    custodianScheme = Crypto.EDDSA_ED25519_SHA512,
                    tokenIssuingEntityEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    tokenIssuingEntityScheme = Crypto.EDDSA_ED25519_SHA512,
                )
            )
            put(
                "right",
                right.toJsonObject(
                    networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isAnonymous = false,
                    networkScheme = Crypto.EDDSA_ED25519_SHA512,
                    custodianEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    custodianScheme = Crypto.EDDSA_ED25519_SHA512,
                    tokenIssuingEntityEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    tokenIssuingEntityScheme = Crypto.EDDSA_ED25519_SHA512,
                )
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        private val alice = TestIdentity.fresh("Alice").party
        private val bob = TestIdentity.fresh("Bob").party

        private val network = Network(
            value = "Network 1",
            operator = alice
        )
        private val anotherNetworkWithDifferentValue = Network(
            value = "Network 2",
            operator = alice
        )
        private val anotherNetworkWithDifferentOperator = Network(
            value = "Network 1",
            operator = bob
        )

        val s = with(network) {
            Network("Network 2", operator)
        }

        val ivnoTokenType = IvnoTokenType(
            network,
            alice,
            bob,
            "Display Name 1",
            1,
            UniqueIdentifier(externalId = "some.id", id = UUID(0, 1))
        )

        private val ivnoTokenTypeWithNetworkOfDifferentValue = ivnoTokenType.copy(
            network = anotherNetworkWithDifferentValue
        )

        private val ivnoTokenTypeWithNetworkOfDifferentOperator = ivnoTokenType.copy(
            network = anotherNetworkWithDifferentOperator
        )

        private val ivnoTokenTypeWithDifferentCustodian = ivnoTokenType.copy(
            custodian = bob
        )

        private val ivnoTokenTypeWithDifferentTokenIssuingEntity = ivnoTokenType.copy(
            tokenIssuingEntity = alice
        )

        private val ivnoTokenTypeWithDifferentDisplayName = ivnoTokenType.copy(
            displayName = "Display Name 2"
        )

        private val ivnoTokenTypeWithDifferentFractionDigits = ivnoTokenType.copy(
            fractionDigits = 2
        )

        private val ivnoTokenTypeWithDifferentLinearId = ivnoTokenType.copy(
            linearId = UniqueIdentifier(externalId = "some.other.id", id = UUID(0, 2))
        )

        @JvmStatic
        fun testData() = listOf(
            Pair(ivnoTokenType, ivnoTokenTypeWithNetworkOfDifferentValue),
            Pair(ivnoTokenType, ivnoTokenTypeWithNetworkOfDifferentOperator),
            Pair(ivnoTokenType, ivnoTokenTypeWithDifferentCustodian),
            Pair(ivnoTokenType, ivnoTokenTypeWithDifferentTokenIssuingEntity),
            Pair(ivnoTokenType, ivnoTokenTypeWithDifferentDisplayName),
            Pair(ivnoTokenType, ivnoTokenTypeWithDifferentFractionDigits),
            Pair(ivnoTokenType, ivnoTokenTypeWithDifferentLinearId),
        )
    }
}