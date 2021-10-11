package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters

data class IssuedTemplateParameters<PRD>(
    val partyTemplateParameters: AbstractPartyTemplateParameters,
    private val productTemplateParameters: PRD
) : TemplateParameters(
    "issued.zn",
    listOf(PartyAndReferenceTemplateParameters(partyTemplateParameters), productTemplateParameters)
) where PRD : TemplateParameters {
    private val issuerTemplateParameters = PartyAndReferenceTemplateParameters(partyTemplateParameters)
    override fun getTargetFilename() = getFileName()
    override fun getReplacements() = getTypeReplacements() +
        issuerTemplateParameters.getTypeReplacements("ISS_") +
        productTemplateParameters.getTypeReplacements("PRD_")

    override val typeName = "Issued${productTemplateParameters.typeName}"
}
