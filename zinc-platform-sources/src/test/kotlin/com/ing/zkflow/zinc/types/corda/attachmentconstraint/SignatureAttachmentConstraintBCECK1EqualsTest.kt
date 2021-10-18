package com.ing.zkflow.zinc.types.corda.attachmentconstraint

import com.ing.zkflow.serialization.bfl.serializers.publickey.BCECSurrogate
import com.ing.zkflow.testing.getZincZKService
import com.ing.zkflow.zinc.types.generateDifferentValueThan
import com.ing.zkflow.zinc.types.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import org.junit.jupiter.api.Test
import java.security.PublicKey

class SignatureAttachmentConstraintBCECK1EqualsTest {
    private val zincZKService = getZincZKService<SignatureAttachmentConstraintBCECK1EqualsTest>()

    @Test
    fun `identity test`() {
        performEqualityTest(attachmentConstraint, attachmentConstraint, true)
    }

    @Test
    fun `different keys should not be equal`() {
        performEqualityTest(attachmentConstraint, otherAttachmentConstraint, false)
    }

    private fun performEqualityTest(
        left: SignatureAttachmentConstraint,
        right: SignatureAttachmentConstraint,
        expected: Boolean
    ) {
        val witness = buildJsonObject {
            put(
                "left",
                left.toJsonObject(BCECSurrogate.ENCODED_SIZE)
            )
            put(
                "right",
                right.toJsonObject(BCECSurrogate.ENCODED_SIZE)
            )
        }.toString()

        zincZKService.run(witness, "$expected")
    }

    companion object {
        private val scheme = Crypto.ECDSA_SECP256K1_SHA256
        private val key: PublicKey = Crypto.generateKeyPair(scheme).public
        private val anotherKey: PublicKey = generateDifferentValueThan(key) {
            Crypto.generateKeyPair(scheme).public
        }

        val attachmentConstraint = SignatureAttachmentConstraint(key)
        val otherAttachmentConstraint = SignatureAttachmentConstraint(anotherKey)
    }
}
