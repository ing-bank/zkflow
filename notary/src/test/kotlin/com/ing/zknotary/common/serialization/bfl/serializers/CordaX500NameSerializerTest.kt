package com.ing.zknotary.common.serialization.bfl.serializers

import com.ing.zknotary.testing.roundTrip
import com.ing.zknotary.testing.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.api.Test

class CordaX500NameSerializerTest {
    @Serializable
    data class Data(val value: @Contextual CordaX500Name)

    @Test
    fun `CordaX500Name serializer`() {
        roundTrip(Data(CordaX500Name("Batman", "UT", "US")))
        sameSize(
            Data(CordaX500Name("Batman", "UT", "US")),
            Data(
                CordaX500Name(
                    "Spidey",
                    "Sups",
                    "Spider-Man",
                    "NY",
                    "NY",
                    "US"
                )
            )
        )
    }
}
