package com.ing.zknotary.common.zkp

import com.ing.zknotary.common.util.includedLastModified
import java.io.File

/**
 * CircuitManager examines Zinc artifacts whether they are up-to-date/outdated.
 *
 * This is achieved by maintaining a metadata file in the artifacts directory.
 * This file has the following format:
 * line 0:timestamp of the latest modification of the source folder content
 * lines 1..n: "path to artifact":"last modification timestamp"
 *
 * line 0 sets a necessary condition for artifacts to be used:
 * if recorded timestamp of the source  < actual timestamp of the source, then artifacts are outdated
 *
 * lines 1..n set a sufficient condition for artifacts to be used:
 * the artifact folder must contain the listed artifacts
 * and their actual timestamps must coincide with the corresponding timestamps.
 * e.g., for line 35: "path/to/artifact":20304050
 * there must be an artifact at "path/to/artifact" and its actual timestamp must be 20304050.
 *
 * Metadata file is constructed when `cache()` is called. It is assumed that
 * at the moment of caching the current artifacts are aligned with the current source.
 */
object CircuitManager {
    const val metadataPath = "__circuit_metadata__"

    enum class Status {
        Outdated,
        UpToDate,
        InProgress
    }

    data class CircuitDescription(val circuitFolder: String, val artifactFolder: String)

    private var circuits = mutableMapOf<CircuitDescription, Status>()

    operator fun get(circuitDescription: CircuitDescription) = circuits[circuitDescription]

    fun register(circuitDescription: CircuitDescription) {
        val (circuitFolder, artifactFolder) = circuitDescription

        val metadataFile = File("$artifactFolder/$metadataPath")

        if (!metadataFile.exists()) {
            circuits[circuitDescription] = Status.Outdated
            return
        }

        // Metadata exists.
        val metadata = metadataFile.readText().split("\n")
        val lastModifiedSourceRecorded = metadata[0].toLong()
        val lastModifiedArtifactsRecorded = metadata.subList(1, metadata.size)

        // Find the last time files in the source folder have been modified.
        val lastModifiedSource = File(circuitFolder).includedLastModified ?: error("No files in the source directory")

        // Check if the source has not been changed after the artifacts have been generated.
        if (lastModifiedSourceRecorded < lastModifiedSource) {
            metadataFile.delete()

            circuits[circuitDescription] = Status.Outdated
            return
        }

        // Check if the artifact folder contains the artifacts as listed in the metadata file.
        for (it in lastModifiedArtifactsRecorded) {
            val (path, lastModified) = it.split(":")
            val artifactFile = File(path)

            if (
                !artifactFile.exists() ||
                lastModified.toLong() < artifactFile.lastModified()
            ) {
                metadataFile.delete()

                circuits[circuitDescription] = Status.Outdated
                return
            }
        }

        // lastModifiedRecorded = lastModifiedSource
        circuits[circuitDescription] = Status.UpToDate
    }

    fun inProgress(circuitDescription: CircuitDescription) {
        circuits[circuitDescription] ?: error("Circuit must be registered first")
        circuits[circuitDescription] = Status.InProgress
    }

    /**
     * Creates the metadata file by
     * 1. Finding out the latest modification date of across files in the circuit folder,
     * 2. Listing all artifacts currently present in the artifacts folder together with their timestamps.
     */
    fun cache(circuitDescription: CircuitDescription) {
        circuits[circuitDescription] ?: error("Circuit must be registered first")

        val (circuitFolder, artifactFolder) = circuitDescription

        // Find the last time files in the source folder have been modified.
        val lastModifiedSource = File(circuitFolder).includedLastModified ?: error("No files in the source directory")

        val metadataFile = File("$artifactFolder/$metadataPath")
        metadataFile.writeText("$lastModifiedSource")

        File(artifactFolder).walkTopDown()
            .filter { it.isFile && it.name != metadataPath }
            .forEach {
                metadataFile.appendText("\n${it.absolutePath}:${it.lastModified()}")
            }

        circuits[circuitDescription] = Status.UpToDate
    }
}
