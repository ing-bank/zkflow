package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import java.nio.file.Path

interface BuildPathProvider {
    fun getBuildPath(metadata: ResolvedZKCommandMetadata): Path

    companion object {
        val Default = BuildPathFromMetadataProvider

        fun withPath(path: Path) = object : BuildPathProvider {
            override fun getBuildPath(metadata: ResolvedZKCommandMetadata) = path
        }
    }
}
