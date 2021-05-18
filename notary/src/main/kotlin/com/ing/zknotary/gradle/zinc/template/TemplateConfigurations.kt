package com.ing.zknotary.gradle.zinc.template

import com.ing.serialization.bfl.serializers.DoubleSurrogate
import com.ing.serialization.bfl.serializers.FloatSurrogate
import com.ing.zknotary.gradle.zinc.template.parameters.AbstractPartyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.AmountTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.AnonymousPartyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.BigDecimalTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.CurrencyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.LinearPointerTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.PartyTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.SecureHashTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.StringTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.UniqueIdentifierTemplateParameters
import com.ing.zknotary.gradle.zinc.template.parameters.X500PrincipalTemplateParameters

open class TemplateConfigurations {
    companion object {
        val floatTemplateParameters = BigDecimalTemplateParameters(
            FloatSurrogate.FLOAT_INTEGER_SIZE.toShort(),
            FloatSurrogate.FLOAT_FRACTION_SIZE.toShort(),
            "Float"
        )
        val doubleTemplateParameters = BigDecimalTemplateParameters(
            DoubleSurrogate.DOUBLE_INTEGER_SIZE.toShort(),
            DoubleSurrogate.DOUBLE_FRACTION_SIZE.toShort(),
            "Double"
        )
    }

    open var stringConfigurations: List<StringTemplateParameters> = emptyList()

    open var bigDecimalConfigurations: List<BigDecimalTemplateParameters> = emptyList()

    open var amountConfigurations: List<AmountTemplateParameters> = emptyList()

    /*
     * Pre-defined collection of configurations to generate zinc sources for
     * standard data types like float and double.
     */
    private val fixedTemplateParameters: List<TemplateParameters> by lazy {
        listOf(
            floatTemplateParameters,
            doubleTemplateParameters,
            UniqueIdentifierTemplateParameters,
            LinearPointerTemplateParameters,
            X500PrincipalTemplateParameters,
            CurrencyTemplateParameters,
            SecureHashTemplateParameters,
        ) +
            AbstractPartyTemplateParameters.all +
            AnonymousPartyTemplateParameters.all +
            PartyTemplateParameters.all
    }

    fun resolveAllTemplateParameters(): List<TemplateParameters> {
        return (fixedTemplateParameters + stringConfigurations + bigDecimalConfigurations + amountConfigurations)
            .flatMap { it.resolveAllConfigurations() }
            .distinct()
    }
}
