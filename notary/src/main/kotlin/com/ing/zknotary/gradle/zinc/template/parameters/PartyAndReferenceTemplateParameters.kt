package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.common.serialization.bfl.serializers.PartyAndReferenceSurrogate
import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

class PartyAndReferenceTemplateParameters(
    private val partyTemplateParameters: AbstractPartyTemplateParameters
) : NamedType, TemplateParameters(
    "party_and_reference.zn",
    listOf(
        ByteArrayTemplateParameters(PartyAndReferenceSurrogate.REFERENCE_SIZE),
        partyTemplateParameters
    )
) {
    override val typeName = partyTemplateParameters.typeName + "AndReference"

    override fun getModuleName() = super.getModuleName().replace("and_reference", "_and_reference")

    override fun getReplacements() = getTypeReplacements() +
        partyTemplateParameters.getTypeReplacements("PARTY_") +
        mapOf(
            "REFERENCE_SIZE" to PartyAndReferenceSurrogate.REFERENCE_SIZE.toString(),
        )

    override fun getTargetFilename() = getFileName()

    companion object {
        val all = AbstractPartyTemplateParameters.all.map {
            PartyAndReferenceTemplateParameters(it)
        }
    }
}
