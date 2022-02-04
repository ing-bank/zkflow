package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import java.util.ServiceLoader

// TODO: check if this is still required for Zinc code generation
object CommandMetadataCache {

    val metadata = mutableMapOf<String, ResolvedZKCommandMetadata>()

    private var metadataIsInitialized = false

    @Synchronized
    private fun init() {
        if (!metadataIsInitialized) {
            ServiceLoader.load(ZKCommandData::class.java).forEach {
                metadata[it.metadata.circuit.name] = it.metadata
            }
            metadataIsInitialized = true
        }
    }

    fun findCommandMetadata(circuitName: String): ResolvedZKCommandMetadata {
        if (!metadataIsInitialized) init()
        return metadata[circuitName] ?: error("Metadata not found for circuit '$circuitName'")
    }
}
