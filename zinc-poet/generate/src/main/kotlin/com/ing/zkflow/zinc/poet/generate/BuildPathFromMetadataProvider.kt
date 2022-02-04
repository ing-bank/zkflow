package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import java.nio.file.Path

class BuildPathFromMetadataProvider : BuildPathProvider {
    override fun getBuildPath(metadata: ResolvedZKCommandMetadata): Path {
        return metadata.circuit.buildFolder.toPath()
    }
}
