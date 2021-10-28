package com.ing.zkflow.zinc.types.java.x500principal

import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toWitness
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import javax.security.auth.x500.X500Principal

class DeserializeX500PrincipalTest {
    private val zincZKService = getZincZKService<DeserializeX500PrincipalTest>()

    @Test
    fun `an X500Principal should be deserialized correctly`() {
        val data = Data(X500Principal("CN=Steve Kille,O=Isode Limited,C=GB"))
        val witness = toWitness(data)

        val expected = data.data.toZincJson()
        zincZKService.run(witness, expected)
    }

    @Serializable
    private data class Data(
        val data: @Contextual X500Principal
    )
}