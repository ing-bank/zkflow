package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import com.ing.zkflow.compilation.zinc.template.NamedType
import com.ing.zkflow.compilation.zinc.template.TemplateParameters

class SignersTemplateParameters(
    metadata: ResolvedZKTransactionMetadata,
) : NamedType, TemplateParameters(
    "signers.zn",
    listOf(
        CollectionTemplateParameters(
            innerTemplateParameters = AbstractPartyTemplateParameters
                .selectAbstractPartyParameters(metadata.network.participantSignatureScheme.schemeCodeName),
            collectionSize = metadata.numberOfSigners
        )
    )
) {
    private val signerTemplateParameters: AbstractPartyTemplateParameters =
        AbstractPartyTemplateParameters.selectAbstractPartyParameters(metadata.network.participantSignatureScheme.schemeCodeName)
    private val collectionTemplateParameters: CollectionTemplateParameters<AbstractPartyTemplateParameters> =
        CollectionTemplateParameters(
            innerTemplateParameters = signerTemplateParameters,
            collectionSize = metadata.numberOfSigners
        )

    override val typeName = "Signers${signerTemplateParameters.typeName}"

    override fun getReplacements() =
        signerTemplateParameters.getTypeReplacements("PARTY_") +
            collectionTemplateParameters.getTypeReplacements("COLLECTION_")

    override fun getTargetFilename() = getFileName()
}
