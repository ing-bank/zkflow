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
            // Collection of participants to MockAsset.
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
    }
}