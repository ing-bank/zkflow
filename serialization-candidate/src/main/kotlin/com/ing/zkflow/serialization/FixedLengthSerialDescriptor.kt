package com.ing.zkflow.serialization

import kotlinx.serialization.descriptors.SerialDescriptor
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
        FixedLengthSerialDescriptor(
            this,
            elementDescriptors.fold(0) { acc, serialDescriptor ->
                acc + serialDescriptor.toFixedLengthSerialDescriptorOrThrow(fullSerialName).byteSize
            }
        )
    }
}
