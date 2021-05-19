package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.WhitelistedByZoneAttachmentConstraintSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.WhitelistedByZoneAttachmentConstraint
import kotlin.reflect.full.findAnnotation

class DeserializeWhitelistedByZoneAttachmentConstraintTest :
    DeserializationTestBase<DeserializeWhitelistedByZoneAttachmentConstraintTest, DeserializeWhitelistedByZoneAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson(
                serialName = WhitelistedByZoneAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService =
        com.ing.zknotary.zinc.types.getZincZKService<DeserializeWhitelistedByZoneAttachmentConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(WhitelistedByZoneAttachmentConstraint),
        )
    }
}
