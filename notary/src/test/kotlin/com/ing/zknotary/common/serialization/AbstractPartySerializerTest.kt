package com.ing.zknotary.common.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

class AbstractPartySerializerTest {
    @Serializable
    data class Data(val value: @Contextual AbstractParty)

    @Test
    fun `serialize and deserialize Party`() {
        val data1 = Data(
            Party(
                CordaX500Name("Company", "UT", "US"),
                generateRSAPubKey()
            )
        )
        val data2 = Data(AnonymousParty(generateRSAPubKey()))

        roundTrip(data1)
        sameSize(data1, data2)
    }

    private fun generateRSAPubKey(): PublicKey {
        val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        return generator.genKeyPair().public
    }
}
