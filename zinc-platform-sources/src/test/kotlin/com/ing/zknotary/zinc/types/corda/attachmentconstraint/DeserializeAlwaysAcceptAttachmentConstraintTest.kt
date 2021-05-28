package com.ing.zknotary.zinc.types.corda.attachmentconstraint

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint

class DeserializeAlwaysAcceptAttachmentConstraintTest :
    DeserializationTestBase<DeserializeAlwaysAcceptAttachmentConstraintTest, DeserializeAlwaysAcceptAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService =
        com.ing.zknotary.zinc.types.getZincZKService<DeserializeAlwaysAcceptAttachmentConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(AlwaysAcceptAttachmentConstraint),
        )
    }
}
