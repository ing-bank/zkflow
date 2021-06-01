package com.ing.zknotary.zinc.types.corda.attachmentconstraint

import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.testing.DeserializationTestBase
import com.ing.zknotary.testing.getZincZKService
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint

class DeserializeAutomaticPlaceholderConstraintTest :
    DeserializationTestBase<DeserializeAutomaticPlaceholderConstraintTest, DeserializeAutomaticPlaceholderConstraintTest.Data>(
        {
            it.data.toZincJson()
        }
    ) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeAutomaticPlaceholderConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(AutomaticPlaceholderConstraint),
        )
    }
}
