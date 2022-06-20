package com.ing.zkflow.common.versioning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class VersionFamilyTest {
    @Test
    fun `latest should return the class of the latest member`() {
        testFamily.latest shouldBe TestStateV3::class
    }

    @Test
    fun `next of v1 should return v2`() {
        testFamily.next(TestStateV1::class) shouldBe TestStateV2::class
    }

    @Test
    fun `next of v2 should return v3`() {
        testFamily.next(TestStateV2::class) shouldBe TestStateV3::class
    }

    @Test
    fun `next of v3 should return null`() {
        testFamily.next(TestStateV3::class) shouldBe null
    }
}
