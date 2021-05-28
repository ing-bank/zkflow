package com.ing.zknotary.zinc.types.corda.attachmentconstraint

import com.ing.zknotary.zinc.types.getZincZKService
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
/**
 * This test has been added to stress out a peculiar behavior encountered when running equality tests for objects being
 * treated as empty structures in zinc.
 *
 * As it can be seen below, in such cases passing an empty JsonObject would produce a successful output (which seems
 * unnatural).
 *
 * On the contrary, if 2 objects are passed as inputs of performEqualityTest() (using the left-right pattern of the rest
 * zinc equality tests), the tests will fail.
 */
class AlwaysAcceptAttachmentConstraintEqualsTest {
    private val zincZKService = getZincZKService<AlwaysAcceptAttachmentConstraintEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest()
    }

    private fun performEqualityTest() {
        val witness = buildJsonObject {}.toString()

        zincZKService.run(witness, "true")
    }
}
