package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.NamedType
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AbstractPartyTemplateParameters(
    val implementationName: String,
    val pkTemplateParameters: PublicKeyTemplateParameters
) : NamedType, TemplateParameters(
    "${implementationName.camelToSnakeCase()}.zn",
    listOf(
        StringTemplateParameters(1), // the serial names of AnonymousParty, and Party, are of length 1
        pkTemplateParameters
    )
) {
    private val pkTypeName by lazy { pkTemplateParameters.typeName }
    private val algName by lazy { pkTypeName.removeSuffix("PublicKey") }
    override val typeName by lazy { "$implementationName$algName" }

    override fun getReplacements() = getTypeReplacements() + pkTemplateParameters.getTypeReplacements("PK_")

    override fun getTargetFilename() = getFileName()

    companion object {
        const val ANONYMOUS_PARTY_TYPE_NAME = "AnonymousParty"
        const val PARTY_TYPE_NAME = "Party"
        val all = PublicKeyTemplateParameters.all.map {
            listOf(
                AbstractPartyTemplateParameters(ANONYMOUS_PARTY_TYPE_NAME, it),
                AbstractPartyTemplateParameters(PARTY_TYPE_NAME, it),
            )
        }.flatten()
    }
}
