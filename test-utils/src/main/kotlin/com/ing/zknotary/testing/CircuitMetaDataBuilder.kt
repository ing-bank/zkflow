package com.ing.zknotary.testing

import com.ing.zknotary.common.zkp.CircuitMetaData
import net.corda.core.contracts.ComponentGroupEnum
import java.io.File

internal data class CircuitMetaDataBuilder(
    var name: String = "UNKNOWN",
    var componentGroupSizes: MutableMap<ComponentGroupEnum, Int> = mutableMapOf(),
    var folder: File = File("/tmp")
) {
    fun name(name: String) = apply { this.name = name }
    fun addComponentGroupSize(componentGroupEnum: ComponentGroupEnum, size: Int) =
        apply { this.componentGroupSizes[componentGroupEnum] = size }
    fun folder(folder: File) = apply { this.folder = folder }
    fun build() = CircuitMetaData(name, componentGroupSizes, folder)
}
