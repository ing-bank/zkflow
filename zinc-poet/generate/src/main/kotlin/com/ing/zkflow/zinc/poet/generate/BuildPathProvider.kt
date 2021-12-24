package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.ResolvedZKTransactionMetadata
import java.nio.file.Path

interface BuildPathProvider {
    fun getBuildPath(metadata: ResolvedZKTransactionMetadata): Path

    companion object {
        val Default = BuildPathFromMetadataProvider()

        fun withPath(path: Path) = object : BuildPathProvider {
            override fun getBuildPath(metadata: ResolvedZKTransactionMetadata) = path
        }
    }
}
