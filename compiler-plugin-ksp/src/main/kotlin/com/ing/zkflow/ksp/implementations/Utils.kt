package com.ing.zkflow.ksp.implementations

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile

/**
 * Set<KSFile> - Set<KSFile> does not work as expected.
 */
private fun Set<KSFile>.complement(that: Set<KSFile>): Set<KSFile> =
    filter { first -> that.none { second -> first.filePath == second.filePath } }.toSet()

/** To compute the set of new files, `resolver.getNewFiles()` ought to be used.
 * Unfortunately, this method fails on Mac.
 * To work around this issue the set of new files is computed.
 *
 * Bug report: https://github.com/google/ksp/issues/820
 * Fixed for Kotlin 1.6.
 */
fun Resolver.getNewFiles(visitedFiles: Set<KSFile>): Set<KSFile> =
    getAllFiles()
        .toSet()
        .complement(visitedFiles)
