package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.SerializerRegistryProvider
import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.zkp.metadata.ResolvedZKCommandMetadata
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import net.corda.core.contracts.CommandData
import org.junit.jupiter.api.Test

internal class GenerateZincCircuitsKtTest {
    @Test
    fun `instantiateZkCommands should not fail for empty list`() {
        val actual = instantiateZkCommands(listOf())
        actual shouldBe emptyList()
    }

    @Test
    fun `instantiateZkCommands should correctly instantiate MyZkCommand`() {
        val mockRegistry = mockk<SerializerRegistryProvider>()
        every { mockRegistry.get() } returns KClassSerializer(MyZkCommand::class, 1, MyZkCommand.serializer())

        val actual = instantiateZkCommands(listOf(mockRegistry))
        actual shouldContain MyZkCommand()
    }

    @Test
    fun `instantiateZkCommands should ignore CommandData that are not ZKCommandData`() {
        val mockRegistry = mockk<SerializerRegistryProvider>()
        every { mockRegistry.get() } returns KClassSerializer(MyCommand::class, 1, MyCommand.serializer())

        val actual = instantiateZkCommands(listOf(mockRegistry))
        actual shouldBe emptyList()
    }
}

@ZKP
class MyZkCommand : ZKCommandData, Versioned {
    override val metadata: ResolvedZKCommandMetadata = commandMetadata { }

    override fun hashCode(): Int {
        return metadata.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is MyZkCommand && metadata == other.metadata
    }
}

@ZKP
class MyCommand : CommandData
