package io.ivno.collateraltoken.zinc.types.membership

import com.ing.zkflow.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zkflow.testing.getZincZKService
import io.ivno.collateraltoken.serialization.ClaimSurrogate
import io.ivno.collateraltoken.zinc.types.membership
import io.ivno.collateraltoken.zinc.types.membershipWithDifferentHolder
import io.ivno.collateraltoken.zinc.types.membershipWithDifferentIdentity
import io.ivno.collateraltoken.zinc.types.membershipWithDifferentLinearId
import io.ivno.collateraltoken.zinc.types.membershipWithDifferentSettings
import io.ivno.collateraltoken.zinc.types.membershipWithDifferentPreviousStateRef
import io.ivno.collateraltoken.zinc.types.membershipWithNetworkOfDifferentOperator
import io.ivno.collateraltoken.zinc.types.membershipWithNetworkOfDifferentValue
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.membership.Membership
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MembershipEqualsTest {
    private val zincZKService = getZincZKService<MembershipEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(membership, membership, true)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different ivnoTokenType should not be equal`(testPair: Pair<Membership, Membership>) {
        performEqualityTest(testPair.first, testPair.second, false)
    }

    private fun performEqualityTest(
        left: Membership,
        right: Membership,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isNetworkAnonymous = false,
                    networkScheme = Crypto.EDDSA_ED25519_SHA512,
                    holderEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isHolderAnonymous = false,
                    holderScheme = Crypto.EDDSA_ED25519_SHA512,
                    identityPropertyLength = ClaimSurrogate.PROPERTY_LENGTH,
                    identityValueLength = DeserializeMembershipTest.IDENTITY_VALUE_LENGTH,
                    settingsValueLength = DeserializeMembershipTest.SETTINGS_VALUE_LENGTH,
                )
            )
            put(
                "right",
                right.toJsonObject(
                    networkEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isNetworkAnonymous = false,
                    networkScheme = Crypto.EDDSA_ED25519_SHA512,
                    holderEncodedSize = EdDSASurrogate.ENCODED_SIZE,
                    isHolderAnonymous = false,
                    holderScheme = Crypto.EDDSA_ED25519_SHA512,
                    identityPropertyLength = ClaimSurrogate.PROPERTY_LENGTH,
                    identityValueLength = DeserializeMembershipTest.IDENTITY_VALUE_LENGTH,
                    settingsValueLength = DeserializeMembershipTest.SETTINGS_VALUE_LENGTH,
                )
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Pair(membership, membershipWithNetworkOfDifferentValue),
            Pair(membership, membershipWithNetworkOfDifferentOperator),
            Pair(membership, membershipWithDifferentHolder),
            Pair(membership, membershipWithDifferentIdentity),
            Pair(membership, membershipWithDifferentSettings),
            Pair(membership, membershipWithDifferentLinearId),
            Pair(membership, membershipWithDifferentPreviousStateRef),
        )
    }
}