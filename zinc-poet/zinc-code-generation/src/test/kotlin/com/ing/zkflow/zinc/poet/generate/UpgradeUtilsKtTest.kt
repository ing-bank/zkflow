package com.ing.zkflow.zinc.poet.generate

import com.ing.zkflow.common.versioning.Versioned
import com.ing.zkflow.common.versioning.ZincUpgrade
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
    }

    @Test
    fun `findUpgradeParameters for encapsulated original should return null`() {
        val actual = findUpgradeParameters(EncapsulatedOriginal::class)
        actual shouldBe null
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
