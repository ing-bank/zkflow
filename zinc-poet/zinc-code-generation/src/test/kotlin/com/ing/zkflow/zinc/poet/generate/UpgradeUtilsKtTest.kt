package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
import com.ing.zkflow.common.zkp.metadata.commandMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

interface Family : Versioned
data class Original(val a: Int) : Family
data class Upgraded(val a: Int, val b: Int) : Family {
    @ZincUpgrade("Self::new(previous.a, 0 as i32)")
    constructor(previous: Original) : this(previous.a, 0)
}

internal class UpgradeUtilsKtTest {
    @Test
    fun `findUpgradeParameters for upgraded should return UpgradeParameters for Upgraded class`() {
        val actual = findUpgradeParameters(Upgraded::class)
        actual?.originalKClass shouldBe Original::class
        actual?.zincUpgradeBody shouldBe "Self::new(previous.a, 0 as i32)"
        actual?.zincUpgradeParameterName shouldBe "previous"
    }

    @Test
    fun `findUpgradeParameters for original should return null`() {
        val actual = findUpgradeParameters(Original::class)
        actual shouldBe null
    }

    @Test
    fun `findUpgradeParameters for encapsulated upgraded should return UpgradeParameters for Upgraded class`() {
        val actual = findUpgradeParameters(EncapsulatedUpgraded::class)
        actual?.originalKClass shouldBe EncapsulatedOriginal::class
        actual?.zincUpgradeBody shouldBe "Self::new(previous.a, 0 as i32)"
        actual?.zincUpgradeParameterName shouldBe "previous"
    }

    @Test
    fun `findUpgradeParameters for encapsulated original should return null`() {
        val actual = findUpgradeParameters(EncapsulatedOriginal::class)
        actual shouldBe null
    }

    @Test
    fun `generateUpgradeVerification should generate successfully for an upgrade command`() {
        val actual = generateUpgradeVerification(MyContract.UpgradeMyStateV1ToMyStateV2().metadata)
        actual.generate() shouldBe """
            mod command_context;
            use command_context::CommandContext;

            mod my_state_v_1;
            use my_state_v_1::MyStateV1;

            mod my_state_v_2;
            use my_state_v_2::MyStateV2;

            fn verify(
                ctx: CommandContext,
            ) -> () {
                let input: MyStateV1 = ctx.inputs.my_state_v_1_0.data;
                let output: MyStateV2 = ctx.outputs.my_state_v_2_0.data;
                
                assert!(output.equals(MyStateV2::upgrade_from(input)), "[UpgradeMyStateV1ToMyStateV2] Not a valid upgrade from MyStateV1 to MyStateV2.");
            }
        """.trimIndent()
    }

    @Test
    fun `generateUpgradeVerification should generate indices successfully for an upgrade command`() {
        val actual = generateUpgradeVerification(
            MyContract.UpgradeMyStateV1ToMyStateV2().commandMetadata {
                circuit {
                    name = this::class.simpleName!!
                }
                numberOfSigners = 1
                command = true
                notary = true
                inputs {
                    private(MyStateV1::class) at 2
                }
                outputs {
                    private(MyStateV2::class) at 3
                }
            }
        )
        actual.generate() shouldBe """
            mod command_context;
            use command_context::CommandContext;

            mod my_state_v_1;
            use my_state_v_1::MyStateV1;

            mod my_state_v_2;
            use my_state_v_2::MyStateV2;

            fn verify(
                ctx: CommandContext,
            ) -> () {
                let input: MyStateV1 = ctx.inputs.my_state_v_1_2.data;
                let output: MyStateV2 = ctx.outputs.my_state_v_2_3.data;
                
                assert!(output.equals(MyStateV2::upgrade_from(input)), "[UpgradeMyStateV1ToMyStateV2] Not a valid upgrade from MyStateV1 to MyStateV2.");
            }
        """.trimIndent()
    }

    @Test
    fun `generateUpgradeVerification should throw when too many inputs`() {
        shouldThrow<IllegalArgumentException> {
            generateUpgradeVerification(
                MyContract.UpgradeMyStateV1ToMyStateV2().commandMetadata {
                    numberOfSigners = 1
                    inputs {
                        private(MyStateV1::class) at 0
                        private(MyStateV1::class) at 1
                    }
                    outputs {
                        private(MyStateV2::class) at 0
                    }
                }
            )
        }.message shouldBe "Upgrade circuit MUST have a single input"
    }

    @Test
    fun `generateUpgradeVerification should throw when no inputs`() {
        shouldThrow<IllegalArgumentException> {
            generateUpgradeVerification(
                MyContract.UpgradeMyStateV1ToMyStateV2().commandMetadata {
                    numberOfSigners = 1
                    outputs {
                        private(MyStateV2::class) at 0
                    }
                }
            )
        }.message shouldBe "Upgrade circuit MUST have a single input"
    }

    @Test
    fun `generateUpgradeVerification should throw when too many outputs`() {
        shouldThrow<IllegalArgumentException> {
            generateUpgradeVerification(
                MyContract.UpgradeMyStateV1ToMyStateV2().commandMetadata {
                    numberOfSigners = 1
                    inputs {
                        private(MyStateV1::class) at 0
                    }
                    outputs {
                        private(MyStateV2::class) at 0
                        private(MyStateV2::class) at 1
                    }
                }
            )
        }.message shouldBe "Upgrade circuit MUST have a single output"
    }

    @Test
    fun `generateUpgradeVerification should throw when no outputs`() {
        shouldThrow<IllegalArgumentException> {
            generateUpgradeVerification(
                MyContract.UpgradeMyStateV1ToMyStateV2().commandMetadata {
                    numberOfSigners = 1
                    inputs {
                        private(MyStateV1::class) at 0
                    }
                }
            )
        }.message shouldBe "Upgrade circuit MUST have a single output"
    }

    companion object {
        interface EncapsulatedFamily : Versioned
        data class EncapsulatedOriginal(val a: Int) : EncapsulatedFamily
        data class EncapsulatedUpgraded(val a: Int, val b: Int) : EncapsulatedFamily {
            @ZincUpgrade("Self::new(previous.a, 0 as i32)")
            constructor(previous: EncapsulatedOriginal) : this(previous.a, 0)
        }
    }
}
