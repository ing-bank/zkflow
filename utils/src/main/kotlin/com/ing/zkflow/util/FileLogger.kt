package com.ing.zkflow.util

import java.io.File
import java.time.Instant
import kotlin.math.min
import kotlin.reflect.KClass

class FileLogger(path: String) {
    companion object {
        fun forClass(klass: KClass<*>) = FileLogger("/${System.getProperty("java.io.tempdir")}/${klass.simpleName ?: error("Cannot create a logger for an anonymous class")}")

        private const val EXCERPT_LENGTH = 500
    }

    private val log = File(path)
    private var i = -1

    fun log(text: String) {
        startIfNeeded()
        log.appendText("$i: $text\n")
    }

    fun logShort(text: String) {
        startIfNeeded()
        log.appendText("$i: ${text.substring(0..min(text.length - 1, EXCERPT_LENGTH))}...\n")
    }

    private fun startIfNeeded() {
        if (i == -1) {
            log.appendText("\n=========${Instant.now()}===================> \n")
        }
        i++
    }
}
