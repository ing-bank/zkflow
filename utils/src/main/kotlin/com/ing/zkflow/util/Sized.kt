package com.ing.zkflow.util

interface BflSized {
    val bitSize: Int
}

data class NodeDescriptor<T : Any>(
    val name: T,
    override val bitSize: Int
) : BflSized {
    override fun toString(): String {
        return "$name: $bitSize bits (${bitSize / Byte.SIZE_BITS} bytes)"
    }
}

val Tree<out BflSized, out BflSized>.bitSize: Int
    get() = when (this) {
        is Tree.Leaf -> this.value.bitSize
        is Tree.Node -> this.value.bitSize
    }
