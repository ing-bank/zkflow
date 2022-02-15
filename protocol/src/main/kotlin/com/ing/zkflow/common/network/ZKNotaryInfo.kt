package com.ing.zkflow.common.network

import com.ing.zkflow.common.zkp.ZKFlow
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.SignatureScheme
import net.corda.core.identity.Party

data class ZKNotaryInfo(
    /**
     * The public key type used by the notary in this network.
     */
    var signatureScheme: SignatureScheme = ZKFlow.DEFAULT_ZKFLOW_SIGNATURE_SCHEME,
) {
    fun validate(notary: Party) {
        require(Crypto.findSignatureScheme(notary.owningKey) == signatureScheme) {
            "Expected the notary to use ${signatureScheme.algorithmName} signature scheme, but found ${notary.owningKey.algorithm}"
        }
    }

    init {
        ZKFlow.requireSupportedSignatureScheme(signatureScheme)
    }
}
