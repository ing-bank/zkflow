package com.ing.zkflow.common.network

import com.ing.zkflow.common.serialization.AttachmentConstraintSerializerRegistry
import com.ing.zkflow.serialization.serializer.corda.CordaX500NameSerializer
import com.ing.zkflow.serialization.serializer.corda.PartySerializer
import com.ing.zkflow.serialization.serializer.corda.PublicKeySerializer
import com.ing.zkflow.serialization.serializer.corda.SecureHashSerializer
import com.ing.zkflow.serialization.serializer.corda.StateRefSerializer

val ZKNetworkParameters.notarySerializer
    get() = PartySerializer(
        cordaSignatureId = notaryInfo.signatureScheme.schemeNumberID,
        cordaX500NameSerializer = CordaX500NameSerializer
    )

val ZKNetworkParameters.stateRefSerializer
    get() = StateRefSerializer(SecureHashSerializer(digestAlgorithm))

val ZKNetworkParameters.signerSerializer
    get() = PublicKeySerializer(participantSignatureScheme.schemeNumberID)

val ZKNetworkParameters.attachmentConstraintSerializer
    get() = AttachmentConstraintSerializerRegistry[attachmentConstraintType.kClass](this)
