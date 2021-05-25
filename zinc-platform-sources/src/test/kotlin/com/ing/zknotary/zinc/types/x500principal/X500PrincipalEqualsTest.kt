package com.ing.zknotary.zinc.types.x500principal

import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import javax.security.auth.x500.X500Principal
import kotlin.time.ExperimentalTime

@ExperimentalTime
class X500PrincipalEqualsTest {
    private val zincZKService = getZincZKService<X500PrincipalEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(alice, alice, true)
    }

    @Test
    fun `different X500Principals should not be equal`() {
        performEqualityTest(alice, steve, false)
    }

    private fun performEqualityTest(
        left: X500Principal,
        right: X500Principal,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put("left", left.toJsonObject())
            put("right", right.toJsonObject())
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val alice = X500Principal("CN=Alice Cooper,C=US")
        val steve = X500Principal("CN=Steve Kille,O=Isode Limited,C=GB")
    }
}
