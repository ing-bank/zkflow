package com.ing.zkflow.util

interface BflSized {
    val bitSize: Int
}

data class NodeDescriptor<T : Any>(
    val description: T,
    override val bitSize: Int
) : BflSized {
    override fun toString(): String {
        return "$description: $bitSize bits (${bitSize / Byte.SIZE_BITS} bytes)"
    }
}

val Tree<BflSized, BflSized>.bitSize: Int
    get() = when (this) {
        is Tree.Leaf -> this.value.bitSize
        is Tree.Node -> this.value.bitSize
    }
