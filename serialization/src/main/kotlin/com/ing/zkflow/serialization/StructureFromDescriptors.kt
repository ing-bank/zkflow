package com.ing.zkflow.serialization

import com.ing.zkflow.serialization.serializer.SizeAnnotation
import com.ing.zkflow.util.BflSized
import com.ing.zkflow.util.NodeDescriptor
import com.ing.zkflow.util.Tree
import com.ing.zkflow.util.Tree.Companion.leaf
import com.ing.zkflow.util.bitSize
import kotlinx.serialization.descriptors.SerialDescriptor

fun String.shorten(): String {
    return split(".")
        .filter { it[0].isUpperCase() }
        .joinToString("_") { it }
}

fun FixedLengthSerialDescriptor.toNodeDescriptor(): NodeDescriptor<String> {
    val size = annotations.filterIsInstance<SizeAnnotation>().firstOrNull()?.value
    val capacityString = size?.let { " (capacity: $it)" } ?: ""
    return NodeDescriptor(serialName.shorten() + capacityString, byteSize * Byte.SIZE_BITS)
}

fun FixedLengthSerialDescriptor.toStructureTree(): Tree<BflSized, BflSized> {
    return Tree.node(toNodeDescriptor()) {
        (0 until elementsCount).map {
            val subTree = toTree(getElementDescriptor(it))
            addNode(
                Tree.node(NodeDescriptor(getElementName(it), subTree.bitSize)) {
                    addNode(subTree)
                }
            )
        }
    }
}

@Suppress("ComplexMethod")
fun toTree(descriptor: SerialDescriptor): Tree<BflSized, BflSized> {
    val fixedLengthDescriptor = when (descriptor) {
        is FixedLengthSerialDescriptor -> descriptor
        else -> try {
            descriptor.toFixedLengthSerialDescriptorOrThrow()
        } catch (e: IllegalStateException) {
            return when (FixedLengthType.tryFromSerialName(descriptor.serialName)) {
                FixedLengthType.LIST -> TODO()
                FixedLengthType.MAP -> TODO()
                FixedLengthType.SET -> TODO()
                FixedLengthType.BYTE_ARRAY -> TODO()
                FixedLengthType.UTF8_STRING -> TODO()
                FixedLengthType.ASCII_STRING -> TODO()
                FixedLengthType.BYTE -> leaf(NodeDescriptor("i8", Byte.SIZE_BITS))
                FixedLengthType.SHORT -> leaf(NodeDescriptor("i16", Short.SIZE_BITS))
                FixedLengthType.INT -> leaf(NodeDescriptor("i32", Int.SIZE_BITS))
                FixedLengthType.LONG -> leaf(NodeDescriptor("i64", Long.SIZE_BITS))
                FixedLengthType.UBYTE -> leaf(NodeDescriptor("u8", UByte.SIZE_BITS))
                FixedLengthType.USHORT -> leaf(NodeDescriptor("u16", UShort.SIZE_BITS))
                FixedLengthType.UINT -> leaf(NodeDescriptor("u32", UInt.SIZE_BITS))
                FixedLengthType.ULONG -> leaf(NodeDescriptor("u64", ULong.SIZE_BITS))
                FixedLengthType.BOOLEAN -> leaf(NodeDescriptor("bool", Byte.SIZE_BITS))
                FixedLengthType.EXACT_LIST -> TODO()
                FixedLengthType.EXACT_BYTE_ARRAY -> TODO()
                null -> TODO()
            }
        }
    }
    return fixedLengthDescriptor.toStructureTree()
}
