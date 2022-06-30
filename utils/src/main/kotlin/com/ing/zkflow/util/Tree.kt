package com.ing.zkflow.util

/**
 * Tree data structure where data of a different type can be attached to nodes and leaves.
 *
 * The [toString] will pretty print the tree, taking special concern for multi-line strings.
 *
 * The following tree:
 * ```kotlin
 * val tree = Tree.node<String, String>("root") {
 *     node("node1") {
 *         leaf("leaf1")
 *         leaf("leaf2")
 *     }
 *     leaf("leaf3\nwith multiple lines")
 * }
 * ```
 * will generate the following output:
 * ```
 * root
 * ├── node1
 * │   ├── leaf1
 * │   └── leaf2
 * └── ╗ leaf3
 *     ║ with multiple lines
 *     ╝
 * ```
 */
sealed interface Tree<NODE, LEAF> {
    val asNode: Node<NODE, LEAF> get() = throw UnsupportedOperationException()
    val asLeaf: Leaf<NODE, LEAF> get() = throw UnsupportedOperationException()

    data class Node<NODE, LEAF>(
        val value: NODE,
        val children: List<Tree<NODE, LEAF>>,
    ) : Tree<NODE, LEAF> {
        override val asNode: Node<NODE, LEAF> = this

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
    ) : Tree<NODE, LEAF> {
        override val asLeaf: Leaf<NODE, LEAF> = this

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

/**
 * Extract value from a [Tree] where nodes and leaves have the same value type.
 */
val <T> Tree<T, T>.value: T
    get() = when (this) {
        is Tree.Leaf<T, T> -> value
        is Tree.Node<T, T> -> value
    }

fun <T, R> Tree<T, T>.map(transform: (T) -> R): Tree<R, R> {
    return when (this) {
        is Tree.Leaf<T, T> -> Tree.leaf(transform(this.value))
        is Tree.Node<T, T> -> Tree.node(transform(this.value)) {
            children.forEach {
                addNode(it.map(transform))
            }
        }
    }
}

/**
 * Returns true _iff_ there is any node or leaf where [predicate] evaluates to true for it's value.
 */
fun <T> Tree<T, T>.anyValue(predicate: (T) -> Boolean): Boolean = when (this) {
    is Tree.Leaf<T, T> -> predicate(value)
    is Tree.Node<T, T> -> this.asNode.children.fold(predicate(value)) { acc, child ->
        acc || predicate(child.value) || child.anyValue(predicate)
    }
}
