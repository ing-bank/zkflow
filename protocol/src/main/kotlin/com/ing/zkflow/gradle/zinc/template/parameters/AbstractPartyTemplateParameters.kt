package com.ing.zkflow.gradle.zinc.template.parameters

import com.ing.zkflow.gradle.zinc.template.TemplateParameters

data class AbstractPartyTemplateParameters(
    val implementationName: String,
    val polyPublicKeyParams: PolyTemplateParameters<PublicKeyTemplateParameters>
) : TemplateParameters(
    "${implementationName.camelToSnakeCase()}.zn",
    listOf(polyPublicKeyParams)
) {
    // Convenience constructor, to simplify configurations in the gradle plugin
    constructor(
        implementationName: String,
        publicKeyParams: PublicKeyTemplateParameters
    ) : this(implementationName, PolyTemplateParameters(publicKeyParams))

    override val typeName by lazy { "$implementationName${polyPublicKeyParams.type.algName}" }

    override fun getReplacements() = getTypeReplacements() + polyPublicKeyParams.getTypeReplacements("PK_")

    override fun getTargetFilename() = getFileName()

    companion object {
        const val ANONYMOUS_PARTY_TYPE_NAME = "AnonymousParty"
        const val PARTY_TYPE_NAME = "Party"

        val allAnonymousParties = PublicKeyTemplateParameters.all.map {
            AbstractPartyTemplateParameters(ANONYMOUS_PARTY_TYPE_NAME, it)
        }

        val allParties = PublicKeyTemplateParameters.all.map {
            AbstractPartyTemplateParameters(PARTY_TYPE_NAME, it)
        }

        val all = allAnonymousParties + allParties

        fun selectAbstractPartyParameters(schemeCodeName: String) =
            AbstractPartyTemplateParameters(
                ANONYMOUS_PARTY_TYPE_NAME,
                PublicKeyTemplateParameters.selectParameters(schemeCodeName)
            )

        fun selectPartyParameters(schemeCodeName: String) =
            AbstractPartyTemplateParameters(
                PARTY_TYPE_NAME,
                PublicKeyTemplateParameters.selectParameters(schemeCodeName)
            )
    }
}
