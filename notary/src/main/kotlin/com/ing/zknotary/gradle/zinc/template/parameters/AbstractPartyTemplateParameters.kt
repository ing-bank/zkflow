package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.PartyType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AbstractPartyTemplateParameters(
    val pkTemplateParameters: PublicKeyTemplateParameters
) : PartyType, TemplateParameters(
    "abstract_party.zn",
    listOf(pkTemplateParameters)
) {
    private val algName by lazy { pkTemplateParameters.typeName.removeSuffix("PublicKey") }
    override val typeName by lazy { "AbstractParty$algName" }

    override fun getReplacements() = getTypeReplacements() +
        pkTemplateParameters.getTypeReplacements("PK_")

    override fun getTargetFilename() = getFileName()

    companion object {
        val all = PublicKeyTemplateParameters.all.map {
            AbstractPartyTemplateParameters(it)
        }
    }
}
