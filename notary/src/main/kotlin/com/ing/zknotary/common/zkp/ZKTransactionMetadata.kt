package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.zkp.ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME
import com.ing.zknotary.common.zkp.ZKFlow.requireSupportedSignatureScheme
import net.corda.core.contracts.AttachmentConstraint
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.SignatureAttachmentConstraint
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import kotlin.reflect.KClass

object ZKFlow {
    val DEFAULT_ZKFLOW_SIGNATURE_SCHEME = Crypto.EDDSA_ED25519_SHA512

    fun requireSupportedSignatureScheme(scheme: SignatureScheme) {
        /**
         * Initially, we only support Crypto.EDDSA_ED25519_SHA512.
         * Later, we may support all scheme supported by Corda: `require(participantSignatureScheme in supportedSignatureSchemes())`
         */
        require(scheme == Crypto.EDDSA_ED25519_SHA512) {
            "Unsupported signature scheme: ${scheme.schemeCodeName}. Only ${Crypto.EDDSA_ED25519_SHA512.schemeCodeName} is supported."
        }
    }
}

@DslMarker
annotation class ZKTransactionMetadataDSL

@ZKCommandMetadataDSL
data class ZKNotary(
    /**
     * The public key type used by the notary in this network.
     */
    var signatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
) {
    init {
        requireSupportedSignatureScheme(signatureScheme)
    }
}

@ZKTransactionMetadataDSL
data class ZKNetwork(
    var participantSignatureScheme: SignatureScheme = DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
    var attachmentConstraintType: KClass<out AttachmentConstraint> = SignatureAttachmentConstraint::class
) {
    var notary = ZKNotary()

    init {
        requireSupportedSignatureScheme(participantSignatureScheme)
    }

    fun notary(init: ZKNotary.() -> Unit) = notary.apply(init)
}

@ZKTransactionMetadataDSL
class ZKCommandList : ArrayList<KClass<out CommandData>>() {
    fun command(command: KClass<out CommandData>): Boolean {
        require(!contains(command)) { "The transaction already contains a '$command'. ZKFLow transactions can only contain one of each command" }

        return add(command)
    }

    operator fun KClass<out CommandData>.unaryPlus() = command(this)
}

@ZKTransactionMetadataDSL
class ZKTransactionMetadata {
    var network = ZKNetwork()
    var commands = ZKCommandList()

    fun network(init: ZKNetwork.() -> Unit): ZKNetwork {
        return network.apply(init)
    }

    fun commands(init: ZKCommandList.() -> Unit): ZKCommandList {
        return commands.apply(init)
    }
}

fun transactionMetadata(init: ZKTransactionMetadata.() -> Unit): ZKTransactionMetadata {
    return ZKTransactionMetadata().apply(init)
}
