package com.ing.zknotary.zinc.types.attachmentconstraint

import com.ing.zknotary.common.serialization.bfl.serializers.SignatureAttachmentConstraintSurrogate
import com.ing.zknotary.common.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zknotary.common.zkp.ZincZKService
import com.ing.zknotary.zinc.types.DeserializationTestBase
import com.ing.zknotary.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import kotlin.reflect.full.findAnnotation

class DeserializeSignatureAttachmentConstraintBCECK1Test :
    DeserializationTestBase<DeserializeSignatureAttachmentConstraintBCECK1Test, DeserializeSignatureAttachmentConstraintBCECK1Test.Data>(
        {
            it.data.toZincJson(
                serialName = SignatureAttachmentConstraintSurrogate::class.findAnnotation<SerialName>()!!.value,
                pkSerialName = BCECSurrogate::class.findAnnotation<SerialName>()!!.value,
                encodedSize = BCECSurrogate.ENCODED_SIZE,
            )
        }
    ) {
    override fun getZincZKService(): ZincZKService =
        com.ing.zknotary.zinc.types.getZincZKService<DeserializeSignatureAttachmentConstraintBCECK1Test>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(SignatureAttachmentConstraint(Crypto.generateKeyPair(Crypto.ECDSA_SECP256K1_SHA256).public)),
        )
    }
}
