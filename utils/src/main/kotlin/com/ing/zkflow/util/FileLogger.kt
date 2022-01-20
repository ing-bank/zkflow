package com.ing.zkflow.util

import java.io.File
import java.util.Stack
import kotlin.math.min
import kotlin.reflect.KClass

class FileLogger(path: String) {
    companion object {
        fun forClass(klass: KClass<*>) = FileLogger("/${System.getProperty("java.io.tempdir")}/${klass.simpleName ?: error("Cannot create a logger for an anonymous class")}")

        private const val EXCERPT_LENGTH = 500
    }

    private val treeBuilderStack: Stack<Tree.Companion.TreeBuilder<String, String>> = Stack()

    private val log = File(path)

    /**
     * Starts a new phase.
     */
    private fun startPhase(name: String) {
        treeBuilderStack.push(Tree.Companion.TreeBuilder(name))
    }

    private fun stopPhase() {
        popFromTreeBuilderStack()
    }

    private fun popFromTreeBuilderStack() {
        if (treeBuilderStack.isNotEmpty()) {
            val tree = treeBuilderStack.pop().build()
            treeBuilderStack.peekOrNull()?.addNode(tree)
                ?: log.appendText("$tree\n")
        }
    }

    /**
     * Run [block] in a new phase with [name].
     */
    fun <T> phase(name: String, block: (FileLogger) -> T): T {
        startPhase(name)
        return try {
            block(this)
        } finally {
            stopPhase()
        }
    }

    fun log(text: String, short: Boolean = false) {
        val sizedText = if (short) {
            text.substring(0..min(text.length - 1, EXCERPT_LENGTH))
        } else {
            text
        }

        doLog(sizedText)
    }

    private fun doLog(message: String) {
        if (treeBuilderStack.isNotEmpty()) {
            treeBuilderStack.peek().leaf(message)
        } else {
            log.appendText("${message}\n")
        }
    }
}

fun <T> Stack<T>.peekOrNull(): T? = if (isNotEmpty()) peek() else null
