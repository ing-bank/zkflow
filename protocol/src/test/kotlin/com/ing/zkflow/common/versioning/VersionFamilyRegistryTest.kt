package com.ing.zkflow.common.versioning

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
    fun `unknown family member should fail`() {
        testFamilyRegistry.familyOf(UnknownState::class) shouldBe null
    }

    @Test
    fun `unknown family should fail`() {
        testFamilyRegistry[UnregisteredFamily::class] shouldBe null
    }
}
