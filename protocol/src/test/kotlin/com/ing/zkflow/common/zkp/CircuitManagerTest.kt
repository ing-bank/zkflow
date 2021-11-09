package com.ing.zkflow.common.zkp

import com.ing.zkflow.common.util.includedLastModified
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class CircuitManagerTest {
    private val source: String = javaClass.getResource("/CircuitManager/src").path
    private val artifact: String = javaClass.getResource("/CircuitManager/artifact").path
    private val id = CircuitManager.CircuitDescription(source, artifact)

    @Test
    fun `no metadata`() {
        CircuitManager.register(id)
        assertEquals(CircuitManager.Status.Outdated, CircuitManager[id])
    }

    @Test
    fun `metadata outdated`() {
        val outdatedMetadataFile = File("$artifact/${CircuitManager.METADATA_PATH}")
        outdatedMetadataFile.writeText("0")

        CircuitManager.register(id)
        assertEquals(CircuitManager.Status.Outdated, CircuitManager[id])
        assert(!outdatedMetadataFile.exists())
    }

    @Test
    fun `metadata present and valid`() {
        // Find the last time files in the source folder have been modified.
        val lastModified = File(source).includedLastModified ?: error("No files in the source directory")

        // Create a dummy artifact.
        val artifactFile = File("$artifact/dummy.txt")
        artifactFile.writeText("Come with me if you want to live")

        val metadataFile = File("$artifact/${CircuitManager.METADATA_PATH}")
        metadataFile.writeText("$lastModified")
        metadataFile.appendText("\n${artifactFile.absolutePath}:${artifactFile.lastModified()}")

        CircuitManager.register(id)
        assertEquals(CircuitManager.Status.UpToDate, CircuitManager[id])

        metadataFile.delete()
        artifactFile.delete()
    }
}
