package com.ing.zkflow.util

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Stack
import kotlin.math.min
import kotlin.reflect.KClass

@SuppressFBWarnings("PATH_TRAVERSAL_IN", justification = "Path is always calculated from class name")
class FileLogger constructor(klass: KClass<*>, withTimeStamp: Boolean = false) {
    private val tmpDir = System.getProperty("java.io.tempdir") ?: "/tmp"
    private val path = when (withTimeStamp) {
        true -> {
            val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH-mm-ss.SSS").withZone(ZoneOffset.UTC).format(Instant.now())
            "/$tmpDir/${klass.simpleName ?: error("Cannot create a logger for an anonymous class")}.$timestamp.log"
        }
        false -> "/$tmpDir/${klass.simpleName ?: error("Cannot create a logger for an anonymous class")}.log"
    }

    private companion object {
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
