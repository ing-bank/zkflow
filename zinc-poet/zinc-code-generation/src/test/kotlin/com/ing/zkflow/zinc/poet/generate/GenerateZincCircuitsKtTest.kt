package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.annotations.ZKP
import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.common.serialization.KClassSerializer
import com.ing.zkflow.common.serialization.KClassSerializerProvider
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
        val mockRegistry = mockk<KClassSerializerProvider>()
        every { mockRegistry.get() } returns KClassSerializer(MyZkCommand::class, 1, MyZkCommand_Serializer)

        val actual = instantiateZkCommands(listOf(mockRegistry))
        actual shouldContain MyZkCommand()
    }

    @Test
    fun `instantiateZkCommands should ignore CommandData that are not ZKCommandData`() {
        val mockRegistry = mockk<KClassSerializerProvider>()
        every { mockRegistry.get() } returns KClassSerializer(MyCommand::class, 1, MyCommand_Serializer)

        val actual = instantiateZkCommands(listOf(mockRegistry))
        actual shouldBe emptyList()
    }
}

@ZKP
class MyZkCommand : ZKCommandData {
    override val metadata: ResolvedZKCommandMetadata = commandMetadata { }

    override fun verifyPrivate(): String = """
        mod module_command_context;
        use module_command_context::CommandContext;
        
        fn verify(ctx: CommandContext) {
            // TODO
        }
    """.trimIndent()

    override fun hashCode(): Int {
        return metadata.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is MyZkCommand && metadata == other.metadata
    }
}

@ZKP
class MyCommand : CommandData
