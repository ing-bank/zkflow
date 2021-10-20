package com.ing.zkflow.zinc.types.corda.attachmentconstraint

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint

class DeserializeAlwaysAcceptAttachmentConstraintTest : DeserializationTestBase <DeserializeAlwaysAcceptAttachmentConstraintTest, DeserializeAlwaysAcceptAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAlwaysAcceptAttachmentConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(AlwaysAcceptAttachmentConstraint),
        )
    }
}
