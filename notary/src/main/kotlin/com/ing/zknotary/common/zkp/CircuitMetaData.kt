package com.ing.zknotary.common.zkp

import net.corda.core.contracts.ComponentGroupEnum
import java.io.File

data class CircuitMetaData(
    val folder: File,

    // TODO this value must be generated from the respective Zinc circuit.
    //  for now, hard-code it.
    /**
     * Hard-bounds on length of component groups.
     * If an enum variant is absent, no bound is present.
     */
    val componentGroupSizes: Map<ComponentGroupEnum, Int> = mapOf(
        ComponentGroupEnum.SIGNERS_GROUP to 2
    )

) {
    init {
        require(folder.exists())
    }
}
