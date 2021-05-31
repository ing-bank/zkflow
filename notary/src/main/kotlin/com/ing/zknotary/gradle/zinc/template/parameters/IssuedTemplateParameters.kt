package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

class IssuedTemplateParameters<PRD>(
    partyTemplateParameters: AbstractPartyTemplateParameters,
    private val productTemplateParameters: PRD
) : NamedType, TemplateParameters(
    "issued.zn",
    listOf(PartyAndReferenceTemplateParameters(partyTemplateParameters), productTemplateParameters)
) where PRD : TemplateParameters,
        PRD : NamedType {
    private val issuerTemplateParameters = PartyAndReferenceTemplateParameters(partyTemplateParameters)
    override fun getTargetFilename() = getFileName()
    override fun getReplacements() = getTypeReplacements() +
        issuerTemplateParameters.getTypeReplacements("ISS_") +
        productTemplateParameters.getTypeReplacements("PRD_")

    override val typeName = "Issued${productTemplateParameters.typeName}"
}
