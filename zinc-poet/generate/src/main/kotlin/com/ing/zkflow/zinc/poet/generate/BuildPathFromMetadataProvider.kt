package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import java.nio.file.Path

class BuildPathFromMetadataProvider : BuildPathProvider {
    override fun getBuildPath(metadata: ResolvedZKTransactionMetadata): Path {
        return metadata.buildFolder.toPath()
    }
}
