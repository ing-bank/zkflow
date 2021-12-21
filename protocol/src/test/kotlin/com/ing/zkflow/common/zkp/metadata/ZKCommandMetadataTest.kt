package com.ing.zkflow.common.zkp.metadata

import com.ing.zkflow.common.contracts.ZKCommandData
import com.ing.zkflow.testing.fixtures.contract.TestContract.TestState
import com.ing.zkflow.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ZKCommandMetadataTest {
    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {
        val cmd = object : ZKCommandData {
            override val metadata = commandMetadata {
                circuit { name = "foo" }

                numberOfSigners = 2

                privateInputs {
                    1 private DummyState::class
                    1 private TestState::class
                }
            }
        }

        cmd.metadata.shouldBeInstanceOf<PrivateResolvedZKCommandMetadata>()
        cmd.metadata.circuit.name shouldBe "foo"
        cmd.metadata.privateInputs.size shouldBe 2
        cmd.metadata.privateInputs.first().type shouldBe DummyState::class
        cmd.metadata.privateInputs.first().index shouldBe 1
    }
}
