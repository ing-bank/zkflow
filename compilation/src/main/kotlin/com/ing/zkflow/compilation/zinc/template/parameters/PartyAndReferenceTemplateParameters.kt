package com.ing.zkflow.compilation.zinc.template.parameters

import com.ing.zkflow.compilation.zinc.template.TemplateParameters
import com.ing.zkflow.serialization.bfl.serializers.PartyAndReferenceSurrogate

data class PartyAndReferenceTemplateParameters(
    private val polyPartyParams: PolyTemplateParameters<AbstractPartyTemplateParameters>
) : TemplateParameters(
    "party_and_reference.zn",
    listOf(
        ByteArrayTemplateParameters(PartyAndReferenceSurrogate.REFERENCE_SIZE),
        polyPartyParams
    )
) {
    // Convenience constructor, to simplify configurations in the gradle plugin
    constructor(
        partyParams: AbstractPartyTemplateParameters
    ) : this(PolyTemplateParameters(partyParams))

    override val typeName = polyPartyParams.type.typeName + "AndReference"

    override fun getModuleName() = super
        .getModuleName()
        .replace("and_reference", "_and_reference")
        .replace("__", "_")

    override fun getReplacements() = getTypeReplacements() +
        polyPartyParams.getTypeReplacements("PARTY_") +
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