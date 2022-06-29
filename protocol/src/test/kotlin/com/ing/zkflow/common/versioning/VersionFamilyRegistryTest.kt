package com.ing.zkflow.common.versioning

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
        shouldThrowExactly<java.lang.IllegalStateException> {
            testFamilyRegistry.familyOf(UnknownState::class)
        }.also { it.message shouldContain "Not registered as family member" }
    }

    @Test
    fun `unknown family should fail`() {
        shouldThrowExactly<java.lang.IllegalStateException> {
            testFamilyRegistry[UnregisteredFamily::class]
        }.also { it.message shouldContain "Not registered as family" }
    }
}
