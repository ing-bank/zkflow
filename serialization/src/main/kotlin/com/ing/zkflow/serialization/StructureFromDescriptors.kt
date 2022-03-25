package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.serializer.SizeAnnotation
import com.ing.zkflow.util.NodeDescriptor
import com.ing.zkflow.util.Tree
import com.ing.zkflow.util.Tree.Companion.leaf
import com.ing.zkflow.util.bitSize
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * This function removes all lowercase elements from a class name, and replaces dots (.) with underscores (_).
 */
internal fun String.shortenClassName(): String {
    return split(".")
        .filter { it[0].isUpperCase() }
        .joinToString("_") { it }
}

val SerialDescriptor.fixedLength: Int?
    get() = annotations.filterIsInstance<SizeAnnotation>().firstOrNull()?.value

internal fun FixedLengthSerialDescriptor.toNodeDescriptor(capacity: Int?): NodeDescriptor<String> {
    val capacityString = capacity?.let { " (capacity: $it)" } ?: ""
    val shortSerialName = serialName.shortenClassName()
    val bitSize = if (shortSerialName == "ArrayList") {
        require(capacity != null) {
            "Capacity MUST be present for ArrayList."
        }
        byteSize * Byte.SIZE_BITS * capacity
    } else {
        byteSize * Byte.SIZE_BITS
    }
    return NodeDescriptor(shortSerialName + capacityString, bitSize)
}

internal fun FixedLengthSerialDescriptor.toStructureTree(parentCapacity: Int?): Tree<NodeDescriptor<String>, NodeDescriptor<String>> {
    require(!(parentCapacity != null && fixedLength != null)) {
        "Only 1 of parentCapacity or SizeAnnotation is allowed for $serialName"
    }
    val capacity = parentCapacity ?: fixedLength
    return Tree.node(toNodeDescriptor(capacity)) {
        (0 until elementsCount).map {
            val elementName = getElementName(it)
            val subTree = if (elementName == "values") {
                toTree(getElementDescriptor(it), capacity)
            } else {
                toTree(getElementDescriptor(it))
            }
            // Exclude nodes with elementName "0", but do include the subTree
            addNode(
                when (elementName) {
                    "0" -> subTree
                    else -> {
                        Tree.node(NodeDescriptor(elementName, subTree.bitSize)) {
                            addNode(subTree)
                        }
                    }
                }
            )
        }
    }
}

@Suppress("ComplexMethod")
fun toTree(descriptor: SerialDescriptor, parentCapacity: Int? = null): Tree<NodeDescriptor<String>, NodeDescriptor<String>> {
    val fixedLengthDescriptor = when (descriptor) {
        is FixedLengthSerialDescriptor -> descriptor
        else -> try {
            descriptor.toFixedLengthSerialDescriptorOrThrow()
        } catch (e: IllegalStateException) {
            return when (FixedLengthType.tryFromSerialName(descriptor.serialName)) {
                FixedLengthType.BYTE -> leaf(NodeDescriptor("i8", Byte.SIZE_BITS))
                FixedLengthType.SHORT -> leaf(NodeDescriptor("i16", Short.SIZE_BITS))
                FixedLengthType.INT -> leaf(NodeDescriptor("i32", Int.SIZE_BITS))
                FixedLengthType.LONG -> leaf(NodeDescriptor("i64", Long.SIZE_BITS))
                FixedLengthType.UBYTE -> leaf(NodeDescriptor("u8", UByte.SIZE_BITS))
                FixedLengthType.USHORT -> leaf(NodeDescriptor("u16", UShort.SIZE_BITS))
                FixedLengthType.UINT -> leaf(NodeDescriptor("u32", UInt.SIZE_BITS))
                FixedLengthType.ULONG -> leaf(NodeDescriptor("u64", ULong.SIZE_BITS))
                FixedLengthType.BOOLEAN -> leaf(NodeDescriptor("bool", Byte.SIZE_BITS))
                FixedLengthType.LIST,
                FixedLengthType.MAP,
                FixedLengthType.SET,
                FixedLengthType.BYTE_ARRAY,
                FixedLengthType.UTF8_STRING,
                FixedLengthType.ASCII_STRING,
                FixedLengthType.EXACT_LIST -> error("Should have been handled by toFixedLengthSerialDescriptorOrThrow")
                null -> error("Not supported: '${descriptor.serialName}'")
            }
        }
    }
    return fixedLengthDescriptor.toStructureTree(parentCapacity)
}
