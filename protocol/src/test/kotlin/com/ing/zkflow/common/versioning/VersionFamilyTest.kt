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
    fun `get by id 2 should return v2`() {
        testFamily.getMember(2) shouldBe TestStateV2::class
    }

    @Test
    fun `get version of v2 should return 2`() {
        testFamily.versionOf(TestStateV2::class) shouldBe 2
    }

    @Test
    fun `support for version higher than latest should be false`() {
        testFamily.supportsVersion(4) shouldBe false
    }

    @Test
    fun `support for version 3 should be false`() {
        testFamily.supportsVersion(3) shouldBe true
    }

    @Test
    fun `next of v3 should return null`() {
        testFamily.next(TestStateV3::class) shouldBe null
    }

    @Test
    fun `highestVersionSupported should return the correct Id`() {
        testFamily.highestVersionSupported shouldBe 3
    }
}
