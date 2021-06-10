package com.ing.zknotary.zinc.types.java.optional

import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.proveTimed
import com.ing.zknotary.zinc.types.setupTimed
import com.ing.zknotary.zinc.types.verifyTimed
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.corda.core.utilities.loggerFor
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class OptionalSomeTest {
    private val log = loggerFor<OptionalSomeTest>()
    private val zincZKService = getZincZKService<OptionalSomeTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc some smoke test`() {
        val left = 42

        val input = buildJsonObject {
            put("value", "$left")
        }.toString()
        val expected = buildJsonObject {
            put("is_none", false)
            put("inner", "$left")
        }.toString()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
