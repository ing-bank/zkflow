package io.ivno.collateraltoken.zinc.types.network

import com.ing.zknotary.common.serialization.bfl.serializers.publickey.EdDSASurrogate
import com.ing.zknotary.testing.getZincZKService
import io.ivno.collateraltoken.zinc.types.toJsonObject
import io.onixlabs.corda.bnms.contract.Network
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.crypto.Crypto
import net.corda.testing.core.TestIdentity
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NetworkEqualsTest {
    private val zincZKService = getZincZKService<NetworkEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(network, network, true)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different network should not be equal`(testPair: Pair<Network, Network>) {
        performEqualityTest(testPair.first, testPair.second, false)
    }

    private fun performEqualityTest(
        left: Network,
        right: Network,
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
        private val operator = TestIdentity.fresh("Alice").party
        private val anotherOperator = TestIdentity.fresh("Bob").party

        val network = Network(
            value = "Network 1",
            operator = operator
        )
        private val anotherNetworkWithDifferentValue = Network(
            value = "Network 2",
            operator = operator
        )
        private val anotherNetworkWithDifferentOperator = Network(
            value = "Network 1",
            operator = anotherOperator
        )

        @JvmStatic
        fun testData() = listOf(
            Pair(network, anotherNetworkWithDifferentValue),
            Pair(network, anotherNetworkWithDifferentOperator),
        )

    }
}