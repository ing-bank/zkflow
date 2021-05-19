package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.HashAttachmentConstraintSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.HashAttachmentConstraint
import net.corda.core.crypto.SecureHash
import kotlin.reflect.full.findAnnotation

class DeserializeHashAttachmentConstraintTest :
    DeserializationTestBase<DeserializeHashAttachmentConstraintTest, DeserializeHashAttachmentConstraintTest.Data>(
        {
            it.data.toZincJson(
                serialName = HashAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService =
        com.ing.zknotary.zinc.types.getZincZKService<DeserializeHashAttachmentConstraintTest>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(HashAttachmentConstraint(SecureHash.randomSHA256())),
        )
    }
}
