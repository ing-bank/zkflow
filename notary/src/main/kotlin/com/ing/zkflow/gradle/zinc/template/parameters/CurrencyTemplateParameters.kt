package com.ing.zkflow.gradle.zinc.template.parameters

import com.ing.serialization.bfl.serializers.CurrencySurrogate
import com.ing.zkflow.gradle.zinc.template.TemplateParameters

object CurrencyTemplateParameters : TemplateParameters(
    "currency.zn",
    listOf(StringTemplateParameters(CurrencySurrogate.CURRENCY_SIZE))
) {
    override fun getReplacements() = mapOf(
        "CURRENCY_CODE_STRING_SIZE" to CurrencySurrogate.CURRENCY_SIZE.toString(),
    )
}
