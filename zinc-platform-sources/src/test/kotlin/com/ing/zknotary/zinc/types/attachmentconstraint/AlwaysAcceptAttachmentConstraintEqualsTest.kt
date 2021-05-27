package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.AlwaysAcceptAttachmentConstraintSurrogate
import com.ing.zknotary.zinc.types.getZincZKService
import com.ing.zknotary.zinc.types.toJsonObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.time.ExperimentalTime

@ExperimentalTime
class AlwaysAcceptAttachmentConstraintEqualsTest {
    private val zincZKService = getZincZKService<AlwaysAcceptAttachmentConstraintEqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(AlwaysAcceptAttachmentConstraint, AlwaysAcceptAttachmentConstraint)
    }

    private fun performEqualityTest(
        left: AttachmentConstraint,
        right: AttachmentConstraint,
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(
                    serialName = AlwaysAcceptAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
                )
            )
            put(
                "right",
                right.toJsonObject(
                    serialName = AlwaysAcceptAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
                )
            )
        }.toString()

        zincZKService.run(witness, "true")
    }
}
