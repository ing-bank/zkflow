package com.ing.zknotary.common.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.identity.CordaX500Name
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
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
