package com.ing.zknotary.common.zkp

import java.io.File
import kotlin.math.max

object CircuitManager {
    /**
     * Metadata file contains:
     * 0: date of the latest modification of the source folder content
     * 1: artifact:content-hash
     * 2..n: ..
     */
    const val metadataPath = "__circuit_metadata__"

    enum class Status {
        NotReady,
        Ready,
        InProgress
    }

    private var circuits = mutableMapOf<Pair<String, String>, Status>()

    operator fun get(circuit: Pair<String, String>) = circuits[circuit]

    fun register(circuit: Pair<String, String>) {
        val (circuitFolder, artifactFolder) = circuit

        val metadataFile = File("$artifactFolder/$metadataPath")

        if (!metadataFile.exists()) {
            circuits[circuit] = Status.NotReady
            return
        }

        // Metadata exists.
        val metadata = metadataFile.readText().split("\n")
        val lastModifiedSourceRecorded = metadata[0].toLong()
        val lastModifiedArtifactsRecorded = metadata.subList(1, metadata.size)

        // Find the last time files in the source folder have been modified.
        val lastModifiedSource = File(circuitFolder).walkTopDown()
            .filter { it.isFile }
            .fold(null as Long?) { lastModified, path ->
                max(path.lastModified(), lastModified ?: 0L)
            } ?: error("No files in the source directory")

        // Check if the artifact folder contains actual artifacts.
        if (lastModifiedSourceRecorded < lastModifiedSource) {
            metadataFile.delete()

            circuits[circuit] = Status.NotReady
            return
        }

        // Check if the artifact folder the correct required artifacts.
        for (it in lastModifiedArtifactsRecorded) {
            val (path, lastModified) = it.split(":")
            val artifactFile = File(path)

            if (
                !artifactFile.exists() ||
                lastModified.toLong() < artifactFile.lastModified()
            ) {
                circuits[circuit] = Status.NotReady
                return
            }
        }

        // lastModifiedRecorded = lastModified
        circuits[circuit] = Status.Ready
    }

    fun inProgress(circuit: Pair<String, String>) {
        circuits[circuit] ?: error("Circuit must be register first")
        circuits[circuit] = Status.InProgress
    }

    fun cache(circuit: Pair<String, String>) {
        circuits[circuit] ?: error("Circuit must be register first")

        val (circuitFolder, artifactFolder) = circuit

        // Find the last time files in the source folder have been modified.
        val lastModified = File(circuitFolder).walkTopDown()
            .filter { it.isFile }
            .fold(null as Long?) { lastModified, path ->
                max(path.lastModified(), lastModified ?: 0L)
            } ?: error("No files in the source directory")

        val metadataFile = File("$artifactFolder/$metadataPath")
        metadataFile.writeText("$lastModified")

        File(artifactFolder).walkTopDown()
            .filter { it.isFile && it.name != metadataPath }
            .forEach {
                metadataFile.appendText("\n${it.absolutePath}:${it.lastModified()}")
            }

        circuits[circuit] = Status.Ready
    }
}
