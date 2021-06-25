package io.ivno.collateraltoken.zinc.types.ivnotokentype

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.contract.IvnoTokenType
import io.ivno.collateraltoken.zinc.types.ivnoTokenType
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithDifferentCustodian
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithDifferentDisplayName
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithDifferentFractionDigits
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithNetworkOfDifferentOperator
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithNetworkOfDifferentValue
import io.ivno.collateraltoken.zinc.types.ivnoTokenTypeWithDifferentTokenIssuingEntity
import io.ivno.collateraltoken.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

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