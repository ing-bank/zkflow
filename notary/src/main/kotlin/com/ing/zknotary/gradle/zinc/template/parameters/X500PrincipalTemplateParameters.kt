package com.ing.zknotary.gradle.zinc.template.parameters

import com.ing.serialization.bfl.serializers.X500PrincipalSurrogate
import com.ing.zknotary.gradle.zinc.template.TemplateParameters

object X500PrincipalTemplateParameters : TemplateParameters(
    "x500_principal.zn",
    listOf(StringTemplateParameters(X500PrincipalSurrogate.PRINCIPAL_SIZE))
) {
    override fun getReplacements() = mapOf(
        "PRINCIPAL_NAME_STRING_SIZE" to X500PrincipalSurrogate.PRINCIPAL_SIZE.toString(),
    )
}
