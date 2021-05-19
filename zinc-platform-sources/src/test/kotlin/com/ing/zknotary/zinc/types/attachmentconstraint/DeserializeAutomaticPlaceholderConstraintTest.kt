package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.AutomaticPlaceholderConstraintSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.AutomaticPlaceholderConstraint
import kotlin.reflect.full.findAnnotation

class DeserializeAutomaticPlaceholderConstraintTest :
    DeserializationTestBase<DeserializeAutomaticPlaceholderConstraintTest, DeserializeAutomaticPlaceholderConstraintTest.Data>(
        {
            it.data.toZincJson(
                serialName = AutomaticPlaceholderConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService =
        com.ing.zknotary.zinc.types.getZincZKService<DeserializeAutomaticPlaceholderConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(AutomaticPlaceholderConstraint),
        )
    }
}
