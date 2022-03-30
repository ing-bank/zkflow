package com.ing.zkflow.ksp.implementations
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFile

/**
 * Set<KSFile> - Set<KSFile> does not work as expected.
 */
private fun Set<KSFile>.complement(that: Set<KSFile>) =
    filter { first -> that.none { second -> first.filePath == second.filePath } }

// To compute the set of new files, `resolver.getNewFiles()` ought to be used.
// Unfortunately, this method fails on Mac.
// To work around this issue the set of new files is computed.
fun Resolver.getNewFiles(visitedFiles: Set<KSFile>): Set<KSFile> {
    val allFiles = getAllFiles().toSet()
    return allFiles.complement(visitedFiles).toSet()
}
