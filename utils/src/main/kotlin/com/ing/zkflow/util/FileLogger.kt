package com.ing.zkflow.util

import java.io.File
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class FileLogger(path: String) {
    companion object {
        fun forClass(klass: KClass<*>) = FileLogger("/${System.getProperty("java.io.tempdir")}/${klass.simpleName ?: error("Cannot create a logger for an anonymous class")}")

        private const val EXCERPT_LENGTH = 500
    }

    private val log = File(path)
    private var i = -1

    private val phaseStack: MutableList<Phase> = mutableListOf()
    private fun outerPrefix(): String =
        phaseStack.take(max(0, phaseStack.lastIndex)).joinToString(separator = "") { it.prefix() }

    /**
     * Run [block] with given [prefix].
     */
    fun <T> withLogPrefix(prefix: String, block: (FileLogger) -> T): T {
        phaseStack.lastOrNull()?.pushPrefix(prefix)
        return try {
            block(this)
        } finally {
            phaseStack.lastOrNull()?.popPrefix()
        }
    }

    /**
     * Starts a new phase.
     */
    private fun startPhase(name: String) {
        val phase = Phase(name).also { log(it.announcement) }
        phaseStack += phase
    }

    private fun stopPhase() {
        phaseStack.removeLastOrNull()
    }

    /**
     * Run [block] in a new phase with [name].
     */
    fun <T> logPhase(name: String, block: (FileLogger) -> T): T {
        startPhase(name)
        return try {
            block(this)
        } finally {
            stopPhase()
        }
    }

    /**
     * If the last phase has the same name, do nothing,
     * otherwise replace the last phase with a new phase.
     *
     * Avoid using this function if possible.
     * Prefer stopping and starting phases.
     */
    fun mergePhase(name: String) {
        if (phaseStack.lastOrNull()?.name != name) {
            stopPhase()
            startPhase(name)
        }
    }

    fun log(text: String, short: Boolean = false) {
        timestamp()

        val sizedText = if (short) {
            text.substring(0..min(text.length - 1, EXCERPT_LENGTH))
        } else {
            text
        }

        val toLog = phaseStack.lastOrNull()?.wrap(outerPrefix(), sizedText) ?: sizedText
        log.appendText("$toLog\n")
    }

    /**
     * Prints a timestamp only once.
     */
    private fun timestamp() {
        if (i == -1) {
            log.appendText("\n=========${Instant.now()}===================> \n")
        }
        i++
    }
}

@Suppress("MagicNumber")
private data class Phase(
    val name: String,
    val prefixStack: MutableList<String> = mutableListOf("   ")
) {
    fun prefix() = prefixStack.joinToString(separator = "", prefix = ">")

    val announcement = ">== [$name] ==<"

    fun wrap(outerPrefix: String, text: String): String {
        val fullPrefix = "$outerPrefix${prefix()}"
        return "$fullPrefix${text.replace("\n", "\n$fullPrefix")}"
    }

    fun pushPrefix(prefix: String) {
        prefixStack += prefix
    }

    fun popPrefix() {
        prefixStack.removeLastOrNull()
    }
}
