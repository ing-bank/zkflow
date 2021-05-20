package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.zknotary.gradle.zinc.template.TemplateParameters

data class AnonymousPartyTemplateParameters(val pkTemplateParameters: PublicKeyTemplateParameters) : TemplateParameters(
    "anonymous_party.zn",
    listOf(pkTemplateParameters)
) {
    private val pkTypeName by lazy { pkTemplateParameters.typeName }
    private val algName by lazy { pkTypeName.removeSuffix("PublicKey") }
    private val pkSnakeCaseType by lazy { pkTypeName.camelToSnakeCase().replace("public_key", "_public_key") }
    private val typeName by lazy { "AnonymousParty$algName" }

    override fun getReplacements() = mapOf(
        "TYPE_NAME" to typeName,
        "CONSTANT_PREFIX" to typeName.camelToSnakeCase().toUpperCase(),
        "PK_TYPE_NAME" to pkTypeName,
        "PK_CONSTANT_PREFIX" to pkSnakeCaseType.toUpperCase(),
        "PK_MODULE_NAME" to pkSnakeCaseType,
    )

    override fun getTargetFilename() = "${typeName.camelToSnakeCase()}.zn"

    companion object {
        val all = PublicKeyTemplateParameters.all.map {
            AnonymousPartyTemplateParameters(it)
        }
    }
}
