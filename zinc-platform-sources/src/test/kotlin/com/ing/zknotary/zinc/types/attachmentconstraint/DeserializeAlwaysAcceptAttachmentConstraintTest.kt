package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.AlwaysAcceptAttachmentConstraintSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AlwaysAcceptAttachmentConstraint
import net.corda.core.contracts.AttachmentConstraint
import kotlin.reflect.full.findAnnotation

class DeserializeAlwaysAcceptAttachmentConstraintTest :
    DeserializationTestBase<DeserializeAlwaysAcceptAttachmentConstraintTest, DeserializeAlwaysAcceptAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson(
                serialName = AlwaysAcceptAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
            )
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
