package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.NamedType
import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.compilation.zinc.util.CircuitConfigurator

class SignersTemplateParameters(
    signersGroup: CircuitConfigurator.SignersGroup
) : NamedType, TemplateParameters(
    "signers.zn",
    listOf(
        CollectionTemplateParameters(
            innerTemplateParameters = AbstractPartyTemplateParameters
                .selectAbstractPartyParameters(signersGroup.signerKeySchemeCodename),
            collectionSize = signersGroup.signerListSize
        )
    )
) {
    private val signerTemplateParameters: AbstractPartyTemplateParameters = AbstractPartyTemplateParameters.selectAbstractPartyParameters(signersGroup.signerKeySchemeCodename)
    private val collectionTemplateParameters: CollectionTemplateParameters<AbstractPartyTemplateParameters> =
        CollectionTemplateParameters(
            innerTemplateParameters = signerTemplateParameters,
            collectionSize = signersGroup.signerListSize
        )

    override val typeName = "Signers${signerTemplateParameters.typeName}"

    override fun getReplacements() =
        signerTemplateParameters.getTypeReplacements("PARTY_") +
            collectionTemplateParameters.getTypeReplacements("COLLECTION_")

    override fun getTargetFilename() = getFileName()
}
