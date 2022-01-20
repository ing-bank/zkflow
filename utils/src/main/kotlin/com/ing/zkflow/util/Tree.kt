package com.ing.zkflow.util

sealed class Tree<NODE, LEAF> {
    data class Node<NODE, LEAF>(
        val value: NODE,
        val children: List<Tree<NODE, LEAF>>,
    ) : Tree<NODE, LEAF>() {
        private fun recursivePrefix(index: Int): String = if (index == children.size - 1) {
            "    "
        } else {
            "│   "
        }
        private fun itemPrefix(index: Int): String = if (index == children.size - 1) {
            "└── "
        } else {
            "├── "
        }

        override fun toString(): String {
            val childrenString: String = children.foldIndexed("") { i, acc, tree ->
                val childString = tree.toString().replace("\n", "\n${recursivePrefix(i)}")
                if (i == 0) {
                    "${itemPrefix(i)}$childString"
                } else {
                    "$acc\n${itemPrefix(i)}$childString"
                }
            }
            val str = value.toString().multiLineString()
            return if (children.isEmpty()) str else "$str\n$childrenString"
        }
    }

    data class Leaf<NODE, LEAF>(
        val value: LEAF,
    ) : Tree<NODE, LEAF>() {
        override fun toString(): String = value.toString().multiLineString()
    }

    companion object {
        class TreeBuilder<N, L>(val value: N) {
            private val children: MutableList<Tree<N, L>> = mutableListOf()

            fun addNode(node: Tree<N, L>) {
                children.add(node)
            }

            fun node(value: N, init: TreeBuilder<N, L>.() -> Unit) {
                val node = TreeBuilder<N, L>(value).apply(init).build()
                children.add(node)
            }

            fun leaf(value: L) {
                children.add(Leaf(value))
            }

            fun build(): Node<N, L> = Node(value, children)
        }

        fun <N, L> node(value: N, init: TreeBuilder<N, L>.() -> Unit): Node<N, L> = TreeBuilder<N, L>(value).apply(init).build()

        fun <N, L> leaf(value: L): Leaf<N, L> = Leaf(value)

        fun String.multiLineString(): String = if (contains("\n")) {
            // this.lineSequence().joinToString("\n│ ", prefix = "┐ ", postfix = "\n┘") { it }
            this.lineSequence().joinToString("\n║ ", prefix = "╗ ", postfix = "\n╝") { it }
        } else this
    }
}
