package com.ing.zkflow.zinc.types.corda.attachmentconstraint

import com.ing.zkflow.common.zkp.ZincZKService
import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.DeserializationTestBase
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.toZincJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto

class DeserializeSignatureAttachmentConstraintBCECK1Test : DeserializationTestBase <DeserializeSignatureAttachmentConstraintBCECK1Test, DeserializeSignatureAttachmentConstraintBCECK1Test.Data>(
    {
        it.data.toZincJson(BCECSurrogate.ENCODED_SIZE)
    }
) {
    override fun getZincZKService(): ZincZKService = getZincZKService<DeserializeSignatureAttachmentConstraintBCECK1Test>()

    @Serializable
    data class Data(val data: @Polymorphic AttachmentConstraint)

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Data(SignatureAttachmentConstraint(Crypto.generateKeyPair(Crypto.ECDSA_SECP256K1_SHA256).public)),
        )
    }
}
