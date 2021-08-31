package com.ing.zknotary.common.zkp

import com.ing.zknotary.testing.fixtures.contract.TestContract.TestState
import com.ing.zknotary.testing.fixtures.state.DummyState
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ZKCommandMetadataTest {
    @Test
    fun `ZKCommandMetadata DSL happy flow works`() {

        val commandMetadata = commandMetadata {
            circuit { name = "foo" }

            numberOfSigners = 2
            private = true

            inputs {
                1 of DummyState::class
                1 of TestState::class
            }
        }

        commandMetadata.circuit?.name shouldBe "foo"
        commandMetadata.inputs.size shouldBe 2
        commandMetadata.inputs.first().type shouldBe DummyState::class
        commandMetadata.inputs.first().count shouldBe 1
    }
}
