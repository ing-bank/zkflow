package com.example

import com.ing.zkflow.compilation.zinc.template.TemplateConfigurations
import com.ing.zkflow.compilation.zinc.template.parameters.AbstractPartyTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.AmountTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.BigDecimalTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.CollectionTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.PublicKeyTemplateParameters
import com.ing.zkflow.compilation.zinc.template.parameters.StringTemplateParameters

@Suppress("MagicNumber") // This class will be thrown away on integration of ZincPoet
object ZKFlowTemplateConfiguration: TemplateConfigurations() {
    init {
        //     // BigDecimal configurations
        //     val bigDecimalTemplateParameters = listOf(
        //         BigDecimalTemplateParameters(24, 6),
        //         BigDecimalTemplateParameters(100, 20),
        //         TemplateConfigurations.floatTemplateParameters,
        //         TemplateConfigurations.doubleTemplateParameters,
        //     )
        //     addConfigurations(bigDecimalTemplateParameters)
        //
        //     // Amount configurations
        //     val amountTemplateParameters = bigDecimalTemplateParameters.map { AmountTemplateParameters(it, 8) }
        //     addConfigurations(amountTemplateParameters)
        //
        //     // String configurations
        //     addConfigurations(StringTemplateParameters(32))
        //
        //     // Issued configurations
        //     addConfigurations(
        //         IssuedTemplateParameters(
        //             AbstractPartyTemplateParameters.selectAbstractPartyParameters(Crypto.EDDSA_ED25519_SHA512.schemeCodeName),
        //             StringTemplateParameters(1)
        //         )
        //     )
        //
        //     addConfigurations(
        //         CollectionTemplateParameters(collectionSize = 3, innerTemplateParameters = StringTemplateParameters(1)),
        //         CollectionTemplateParameters<TemplateParameters>(
        //             "collection_integer.zn",
        //             collectionSize = 3,
        //             platformModuleName = "u32"
        //         ),
        //         CollectionTemplateParameters<TemplateParameters>(
        //             "collection_integer.zn",
        //             collectionSize = 2,
        //             platformModuleName = "i32"
        //         )
        //     )
        //
            // Collection of participants to TestState.
            addConfigurations(
                CollectionTemplateParameters(
                    collectionSize = 1,
                    innerTemplateParameters = AbstractPartyTemplateParameters(
                        AbstractPartyTemplateParameters.ANONYMOUS_PARTY_TYPE_NAME,
                        PublicKeyTemplateParameters.eddsaTemplateParameters
                    )
                ),
                CollectionTemplateParameters(
                    collectionSize = 2,
                    innerTemplateParameters = AbstractPartyTemplateParameters(
                        AbstractPartyTemplateParameters.ANONYMOUS_PARTY_TYPE_NAME,
                        PublicKeyTemplateParameters.eddsaTemplateParameters
                    )
                )
            )
        //
        //     addConfigurations(
        //         MapTemplateParameters(
        //             "StringToIntMap",
        //             6,
        //             StringTemplateParameters(5),
        //             IntegerTemplateParameters.i32
        //         )
        //     )
    }
}