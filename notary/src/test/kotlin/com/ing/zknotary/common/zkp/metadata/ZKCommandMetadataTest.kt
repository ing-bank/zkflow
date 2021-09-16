package com.ing.zknotary.common.zkp.metadata

import com.ing.zknotary.common.contracts.ZKCommandData
import com.ing.zknotary.testing.fixtures.contract.TestContract.TestState
import com.ing.zknotary.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ZKCommandMetadataTest {
    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {
        val cmd = object : ZKCommandData {
            override val metadata = commandMetadata {
                circuit { name = "foo" }

                numberOfSigners = 2
                private = true

                inputs {
                    1 of DummyState::class
                    1 of TestState::class
                }
            }
        }

        cmd.metadata.circuit?.name shouldBe "foo"
        cmd.metadata.inputs.size shouldBe 2
        cmd.metadata.inputs.first().type shouldBe DummyState::class
        cmd.metadata.inputs.first().count shouldBe 1
    }
}
