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
class OptionalNoneTest {
    private val log = loggerFor<OptionalNoneTest>()
    private val zincZKService = getZincZKService<OptionalNoneTest>()

    init {
        zincZKService.setupTimed(log)
    }

    @AfterAll
    fun `remove zinc files`() {
        zincZKService.cleanup()
    }

    @Test
    fun `zinc none smoke test`() {
        val input = "{}"
        val expected = buildJsonObject {
            put("is_none", true)
            put("inner", "0")
        }.toString()

        zincZKService.proveTimed(input, log).let {
            zincZKService.verifyTimed(it, expected, log)
        }
    }
}
