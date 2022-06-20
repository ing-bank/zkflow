package com.ing.zkflow.common.versioning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class VersionFamilyRegistryTest {
    @Test
    fun `version family should be retrievable by family class`() {
        testFamilyRegistry[TestFamily::class] shouldBe testFamily
    }

    @Test
    fun `version family should be retrievable by member class`() {
        testFamilyRegistry.familyOf(TestStateV2::class) shouldBe testFamily
    }

    @Test
    fun `getIdOfLatest should return the correct Id`() {
        testFamilyRegistry.getIdOfLatest(TestStateV1::class) shouldBe TestStateV3::class.hashCode()
    }

    @Test
    fun `getIdOfLAtest for unknown class should raise ClassNotRegistered`() {
        shouldThrow<IllegalStateException> {
            testFamilyRegistry.getIdOfLatest(UnknownState::class)
        }.message shouldBe "Not a family member class com.ing.zkflow.common.versioning.UnknownState"
    }
}
