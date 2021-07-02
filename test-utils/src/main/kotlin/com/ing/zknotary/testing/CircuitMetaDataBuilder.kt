package com.ing.zknotary.testing

import com.ing.zknotary.common.zkp.CircuitMetaData
import net.corda.core.contracts.ComponentGroupEnum
import net.corda.core.utilities.seconds
import java.io.File
import java.time.Duration

internal data class CircuitMetaDataBuilder(
    var name: String = "UNKNOWN",
    var componentGroupSizes: MutableMap<ComponentGroupEnum, Int> = mutableMapOf(),
    var javaClass2ZincType: MutableMap<String, String> = mutableMapOf(),
    var folder: File = File("/tmp"),

    var buildTimeout: Duration = 120.seconds,
    var setupTimeout: Duration = 3000.seconds,
    var provingTimeout: Duration = 300.seconds,
    var verificationTimeout: Duration = 3.seconds,
) {
    fun name(name: String) = apply { this.name = name }
    fun addComponentGroupSize(componentGroupEnum: ComponentGroupEnum, size: Int) =
        apply { this.componentGroupSizes[componentGroupEnum] = size }
    fun folder(folder: File) = apply { this.folder = folder }
    fun buildTimeout(timeout: Duration) = apply { this.buildTimeout = timeout }
    fun setupTimeout(timeout: Duration) = apply { this.setupTimeout = timeout }
    fun provingTimeout(timeout: Duration) = apply { this.provingTimeout = timeout }
    fun verificationTimeout(timeout: Duration) = apply { this.verificationTimeout = timeout }
    fun associateJavaClass2ZincType(javaClass: String, zincType: String) = apply { javaClass2ZincType[javaClass] = zincType }
    fun build() = CircuitMetaData(
        name, componentGroupSizes, javaClass2ZincType,
        folder,
        buildTimeout, setupTimeout, provingTimeout, verificationTimeout
    )
}
