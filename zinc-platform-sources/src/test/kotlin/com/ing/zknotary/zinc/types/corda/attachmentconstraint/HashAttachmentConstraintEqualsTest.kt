package com.ing.zknotary.zinc.types.corda.attachmentconstraint

import com.ing.zknotary.common.crypto.ZINC
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.crypto.SecureHash
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class HashAttachmentConstraintEqualsTest {
    private val zincZKService = getZincZKService<HashAttachmentConstraintEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(attachmentConstraint, attachmentConstraint, true)
    }

    @Test
    fun `different keys should not be equal`() {
        performEqualityTest(attachmentConstraint, otherAttachmentConstraint, false)
    }

    private fun performEqualityTest(
        left: HashAttachmentConstraint,
        right: HashAttachmentConstraint,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject()
            )
            put(
                "right",
                right.toJsonObject()
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        val attachmentConstraint = HashAttachmentConstraint(SecureHash.randomSHA256())
        val otherAttachmentConstraint = HashAttachmentConstraint(SecureHash.random(SecureHash.ZINC))
    }
}
