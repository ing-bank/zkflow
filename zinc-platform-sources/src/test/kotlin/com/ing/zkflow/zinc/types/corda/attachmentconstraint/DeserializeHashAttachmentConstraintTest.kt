package com.ing.zkflow.zinc.types.corda.attachmentconstraint

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.crypto.SecureHash

class DeserializeHashAttachmentConstraintTest : DeserializationTestBase <DeserializeHashAttachmentConstraintTest, DeserializeHashAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeHashAttachmentConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(HashAttachmentConstraint(SecureHash.randomSHA256())),
        )
    }
}
