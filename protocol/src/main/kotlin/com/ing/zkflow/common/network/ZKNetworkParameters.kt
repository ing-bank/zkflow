package com.ing.zkflow.common.network

import com.ing.zkflow.common.serialization.AttachmentConstraintSerializerRegistry
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
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

val ZKNetworkParameters.notarySerializer
    get() = PartySerializer(
        cordaSignatureId = notaryInfo.signatureScheme.schemeNumberID,
        cordaX500NameSerializer = CordaX500NameSerializer
    )

val ZKNetworkParameters.signerSerializer
    get() = PublicKeySerializer(participantSignatureScheme.schemeNumberID)

val ZKNetworkParameters.attachmentConstraintSerializer
    get() = AttachmentConstraintSerializerRegistry[attachmentConstraintType.kClass](this)
