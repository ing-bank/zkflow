package com.ing.zkflow.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors

/**
 * Attach size in bytes to kotlinx's [SerialDescriptor]
 */
class FixedLengthSerialDescriptor(
    descriptor: SerialDescriptor,
    val byteSize: Int,
) : SerialDescriptor by descriptor

/**
 * Convenience function attempting to convert a [SerialDescriptor] to [FixedLengthSerialDescriptor] by inspection.
 */
fun SerialDescriptor.toFixedLengthSerialDescriptorOrThrow(parentSerialName: String? = null): FixedLengthSerialDescriptor {
    val fullSerialName = "${if (parentSerialName != null) "$parentSerialName." else ""}$serialName"
    return if (this is FixedLengthSerialDescriptor) {
        this
    } else {
        if (elementsCount == 0 && kind != StructureKind.CLASS) {
            // If no elements are present we cannot attach a size to the descriptor rendering it impossible
            // to convert `SerialDescriptor` to `FixedLengthSerialDescriptor`
            error(" `SerialDescriptor` `$fullSerialName` cannot be converted to `FixedLengthSerialDescriptor`")
        }
        FixedLengthSerialDescriptor(
            this,
            elementDescriptors.fold(0) { acc, serialDescriptor ->
                acc + serialDescriptor.toFixedLengthSerialDescriptorOrThrow(fullSerialName).byteSize
            }
        )
    }
}
