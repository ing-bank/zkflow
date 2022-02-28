package com.ing.zkflow.common.network

import net.corda.core.crypto.DigestAlgorithm
import net.corda.core.crypto.SignatureScheme

interface ZKNetworkParameters {
    /**
     * The version of these ZKNetworkParameters.
     *
     * This is used to load the networkparameters used to create a transaction when it is deserialized from storage
     * or when it is received in a backchain.
     */
    val version: Int

    /**
     * The participant [SignatureScheme] type required by this ZKFlow CorDapp. All parties on the network must use this scheme.
     */
    val participantSignatureScheme: SignatureScheme

    /**
     * The attachment constraint type required by this ZKFlow CorDapp for all states on its network.
     */
    val attachmentConstraintType: ZKAttachmentConstraintType

    val notaryInfo: ZKNotaryInfo

    /**
     * The digest algorithm to use for transaction Merkle tree hashing.
     */
    val digestAlgorithm: DigestAlgorithm

    /**
     * The serialization scheme to use for transaction component serialization.
     */
    val serializationSchemeId: Int
}
